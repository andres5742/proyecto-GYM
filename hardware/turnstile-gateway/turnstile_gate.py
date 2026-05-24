#!/usr/bin/env python3
"""
Control del SEGURO del torniquete (placa CAFE en COM3 @ 19200, protocolo ATP).

Reglas:
  - Al arrancar: bloquear (h + i).
  - Acceso GRANTED + gateOpened: liberar (a) unos segundos y volver a bloquear.
  - Denegado / inactivo / sin afiliado: mantener bloqueado.
"""
from __future__ import annotations

import json
import os
import sys
import threading
import time
import urllib.error
import urllib.request

try:
    import serial
except ImportError:
    serial = None  # type: ignore

_GATE_DIR = os.path.dirname(os.path.abspath(__file__))
_PENDING_FILE = os.path.join(_GATE_DIR, ".gate-pending.json")
_LOG_FILE = os.path.join(_GATE_DIR, "gate.log")
_relock_timer: threading.Timer | None = None
_active_ser = None
_serial_lock = threading.Lock()

MODE = "none"
GATE_PORT = ""
GATE_BAUD = 9600
READER_BAUD = 9600
GATE_PROTOCOL = ""
LOCK_BYTES_LIST: list[bytes] = []
LOCK_BYTES = b""
UNLOCK_BYTES = b""
UNLOCK_MS = 8000
HTTP_LOCK = ""
HTTP_UNLOCK = ""


def set_active_serial(ser) -> None:
    global _active_ser
    _active_ser = ser


def clear_active_serial() -> None:
    global _active_ser
    _active_ser = None


def _load_gate_env_file() -> None:
    path = os.path.join(_GATE_DIR, "turnstile-gate.env")
    if not os.path.isfile(path):
        example = os.path.join(_GATE_DIR, "turnstile-gate.env.example")
        if os.path.isfile(example):
            try:
                with open(example, encoding="utf-8") as src, open(path, "w", encoding="utf-8") as dst:
                    dst.write(src.read())
            except OSError:
                pass
        path = os.path.join(_GATE_DIR, "turnstile-gate.env")
    if not os.path.isfile(path):
        return
    with open(path, encoding="utf-8") as fh:
        for raw in fh:
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            key, value = key.strip(), value.strip()
            if key:
                os.environ[key] = value


def _hex_bytes(name: str) -> bytes:
    raw = os.environ.get(name, "").replace(" ", "").strip()
    if not raw:
        return b""
    return bytes.fromhex(raw)


def _parse_lock_bytes() -> list[bytes]:
    multi = os.environ.get("TURNSTILE_LOCK_CHARS", "").strip()
    if multi:
        return [ch.encode("ascii")[:1] for ch in multi if ch.strip()]
    single = os.environ.get("TURNSTILE_LOCK_CHAR", "").strip()
    if single:
        return [single.encode("ascii")[:1]]
    return [ch.encode("ascii")[:1] for ch in "hei"]


def _read_config() -> None:
    global MODE, GATE_PORT, GATE_BAUD, LOCK_BYTES, LOCK_BYTES_LIST, UNLOCK_BYTES, UNLOCK_MS
    global GATE_PROTOCOL, READER_BAUD, HTTP_LOCK, HTTP_UNLOCK
    _load_gate_env_file()
    MODE = os.environ.get("TURNSTILE_GATE_MODE", "serial").lower().strip()
    GATE_PORT = os.environ.get("TURNSTILE_GATE_PORT", "").strip() or os.environ.get("SERIAL_PORT", "COM3").strip()
    GATE_PROTOCOL = os.environ.get("TURNSTILE_GATE_PROTOCOL", "atp-acceso").lower().strip()
    READER_BAUD = int(os.environ.get("SERIAL_BAUD", "9600"))
    GATE_BAUD = int(os.environ.get("TURNSTILE_GATE_BAUD", "19200"))

    if GATE_PROTOCOL in ("atp", "atp-acceso", "atp4", "atp-acceso-4"):
        LOCK_BYTES_LIST = _parse_lock_bytes()
        LOCK_BYTES = LOCK_BYTES_LIST[0] if LOCK_BYTES_LIST else b"h"
        unlock_ch = os.environ.get("TURNSTILE_UNLOCK_CHAR", "a").strip() or "a"
        UNLOCK_BYTES = unlock_ch.encode("ascii")[:1]
    else:
        LOCK_BYTES_LIST = _parse_lock_bytes()
        LOCK_BYTES = LOCK_BYTES_LIST[0] if LOCK_BYTES_LIST else b""
        LOCK_BYTES = _hex_bytes("TURNSTILE_LOCK_BYTES") or LOCK_BYTES
        if LOCK_BYTES and not LOCK_BYTES_LIST:
            LOCK_BYTES_LIST = [LOCK_BYTES]
        UNLOCK_BYTES = _hex_bytes("TURNSTILE_UNLOCK_BYTES")
        if not UNLOCK_BYTES and os.environ.get("TURNSTILE_UNLOCK_CHAR", "").strip():
            UNLOCK_BYTES = os.environ.get("TURNSTILE_UNLOCK_CHAR", "a").encode("ascii")[:1]
        if not LOCK_BYTES_LIST and LOCK_BYTES:
            LOCK_BYTES_LIST = [LOCK_BYTES]

    UNLOCK_MS = int(os.environ.get("TURNSTILE_UNLOCK_MS", "8000"))
    HTTP_LOCK = os.environ.get("TURNSTILE_HTTP_LOCK", "").strip()
    HTTP_UNLOCK = os.environ.get("TURNSTILE_HTTP_UNLOCK", "").strip()


_read_config()


def _payload_label(payload: bytes) -> str:
    if len(payload) == 1 and 32 <= payload[0] <= 126:
        return f"ascii '{chr(payload[0])}' hex={payload.hex()}"
    return payload.hex()


def _log(msg: str) -> None:
    line = f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] SEGURO: {msg}"
    print(line, flush=True)
    try:
        with open(_LOG_FILE, "a", encoding="utf-8") as fh:
            fh.write(line + "\n")
    except OSError:
        pass


def _write_on_serial(ser, payload: bytes) -> None:
    ser.reset_output_buffer()
    ser.reset_input_buffer()
    ser.write(payload)
    ser.flush()
    time.sleep(0.28)


def _send_serial_once(payload: bytes, label: str) -> bool:
    if not payload or not GATE_PORT:
        return False
    if serial is None:
        _log("Instale pyserial: pip install pyserial")
        return False

    with _serial_lock:
        if _active_ser is not None and getattr(_active_ser, "is_open", False):
            try:
                old_baud = _active_ser.baudrate
                if GATE_BAUD != old_baud:
                    _active_ser.baudrate = GATE_BAUD
                    time.sleep(0.22)
                _write_on_serial(_active_ser, payload)
                if GATE_BAUD != old_baud:
                    _active_ser.baudrate = old_baud
                    time.sleep(0.12)
                _log(f"{label} -> {GATE_PORT} @ {GATE_BAUD} via lector ({_payload_label(payload)})")
                return True
            except serial.SerialException as ex:
                _log(f"{label} error puerto compartido: {ex}")
                return False

        try:
            with serial.Serial(GATE_PORT, GATE_BAUD, timeout=1, write_timeout=2) as ser:
                _write_on_serial(ser, payload)
            _log(f"{label} -> {GATE_PORT} @ {GATE_BAUD} directo ({_payload_label(payload)})")
            return True
        except serial.SerialException as ex:
            _log(f"{label} error en {GATE_PORT}: {ex}")
            return False


def _send_serial(payload: bytes, label: str) -> bool:
    for attempt in range(1, 4):
        if _send_serial_once(payload, label if attempt == 1 else f"{label} (intento {attempt})"):
            return True
        time.sleep(0.35)
    return False


def _post_http(url: str, label: str) -> bool:
    if not url:
        return False
    body = json.dumps({"action": "lock" if "lock" in label.lower() else "open"}).encode()
    req = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            _log(f"{label} -> {url} ({resp.status})")
            return True
    except Exception as ex:
        _log(f"{label} HTTP error {url}: {ex}")
        return False


def _apply_lock_payloads() -> bool:
    if MODE != "serial":
        return False
    payloads = LOCK_BYTES_LIST or ([LOCK_BYTES] if LOCK_BYTES else [])
    if not payloads:
        _log("Configure TURNSTILE_LOCK_CHARS=hei y TURNSTILE_UNLOCK_CHAR=a")
        return False
    ok = True
    for index, payload in enumerate(payloads):
        label = "PONER seguro" if index == 0 else f"PONER seguro ({chr(payload[0])})"
        if not _send_serial(payload, label):
            ok = False
        if index < len(payloads) - 1:
            time.sleep(0.28)
    return ok


def lock_gate() -> bool:
    global _relock_timer
    _read_config()
    if _relock_timer:
        _relock_timer.cancel()
        _relock_timer = None
    if MODE == "serial":
        return _apply_lock_payloads()
    if MODE == "http":
        return _post_http(HTTP_LOCK, "PONER seguro")
    if MODE == "none":
        _log("Modo none — revise turnstile-gate.env (TURNSTILE_GATE_MODE=serial)")
    return False


def unlock_gate(ms: int | None = None) -> bool:
    global _relock_timer
    _read_config()
    duration = UNLOCK_MS if ms is None else ms
    ok = False
    if MODE == "serial":
        if UNLOCK_BYTES:
            ok = _send_serial(UNLOCK_BYTES, "QUITAR seguro")
        else:
            _log("Configure TURNSTILE_UNLOCK_CHAR=a")
            return False
    elif MODE == "http":
        ok = _post_http(HTTP_UNLOCK, "QUITAR seguro")
    else:
        _log("Modo none — no se puede quitar seguro")
        return False

    if not ok:
        return False

    if _relock_timer:
        _relock_timer.cancel()

    def _relock() -> None:
        lock_gate()

    _relock_timer = threading.Timer(max(1, duration) / 1000.0, _relock)
    _relock_timer.daemon = True
    _relock_timer.start()
    _log(f"Re-seguro automatico en {duration} ms")
    return True


def lock_standalone(retries: int = 3) -> bool:
    """Bloqueo inicial antes de abrir el lector (COM3 libre)."""
    _read_config()
    if MODE != "serial":
        return False
    if _active_ser is not None and getattr(_active_ser, "is_open", False):
        return lock_gate()
    for attempt in range(1, retries + 1):
        _log(f"Bloqueo inicial torniquete (intento {attempt}/{retries})")
        if _apply_lock_payloads():
            return True
        time.sleep(0.6)
    return False


def startup_lock(retries: int = 3) -> bool:
    """Al abrir el lector: asegurar torniquete bloqueado."""
    ok = False
    for attempt in range(1, retries + 1):
        if lock_gate():
            ok = True
            break
        time.sleep(0.5)
    if ok:
        _log("Torniquete bloqueado al iniciar lector")
    else:
        _log("AVISO: no se pudo bloquear torniquete al iniciar")
    return ok


def queue_gate_command(cmd: str, data: dict | None = None) -> None:
    payload = {"cmd": cmd, "data": data, "ts": time.time()}
    try:
        with open(_PENDING_FILE, "w", encoding="utf-8") as fh:
            json.dump(payload, fh)
    except OSError as ex:
        _log(f"No se pudo encolar {cmd}: {ex}")


def request_lock() -> None:
    """CLI / scripts externos: bloquea o encola si COM3 esta ocupado."""
    if not lock_gate():
        queue_gate_command("lock")


def sync_access_result(result: str, gate_opened: bool = False) -> None:
    result_u = str(result or "").upper()
    if result_u == "SELECT_MEMBER":
        lock_gate()
    elif result_u == "GRANTED" and gate_opened:
        unlock_gate()
    else:
        lock_gate()


def consume_pending_gate() -> bool:
    if not os.path.isfile(_PENDING_FILE):
        return False
    try:
        with open(_PENDING_FILE, encoding="utf-8") as fh:
            payload = json.load(fh)
        os.remove(_PENDING_FILE)
    except (OSError, json.JSONDecodeError) as ex:
        _log(f"Orden pendiente invalida: {ex}")
        try:
            os.remove(_PENDING_FILE)
        except OSError:
            pass
        return False

    cmd = str(payload.get("cmd", "")).lower()
    if cmd == "lock":
        lock_gate()
    elif cmd == "unlock":
        unlock_gate()
    elif cmd == "sync":
        data = payload.get("data") or {}
        sync_access_result(str(data.get("result", "")), bool(data.get("gateOpened", False)))
    else:
        _log(f"Orden pendiente desconocida: {cmd}")
    return True


def after_api_response(body: str) -> None:
    _read_config()
    try:
        data = json.loads(body)
    except json.JSONDecodeError:
        lock_gate()
        return
    sync_access_result(str(data.get("result", "")), bool(data.get("gateOpened", False)))


def main() -> None:
    _read_config()
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(0)
    cmd = sys.argv[1].lower()
    if cmd == "lock":
        request_lock()
    elif cmd == "unlock":
        unlock_gate()
    elif cmd == "startup":
        ok = lock_standalone(15)
        sys.exit(0 if ok else 1)
    elif cmd == "after-api" and len(sys.argv) > 2:
        after_api_response(sys.argv[2])
    else:
        print("Uso: turnstile_gate.py lock|unlock|startup|after-api '{json}'")
        sys.exit(1)


if __name__ == "__main__":
    main()
