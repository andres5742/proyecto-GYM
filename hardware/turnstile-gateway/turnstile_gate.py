#!/usr/bin/env python3
"""
Control del SEGURO del torniquete (placa CAFÉ; la azul es solo I/O del PC).

Comportamiento deseado:
  - Al iniciar la app: PONER seguro (bloqueado).
  - Usuario activo + GRANTED: QUITAR seguro unos segundos, luego volver a poner.
  - Denegado o inactivo: mantener seguro.

Configure turnstile-gate.env (copie turnstile-gate.env.example) o iniciar-lector-tarjeta.bat:
  TURNSTILE_GATE_MODE=serial|http|none
  TURNSTILE_GATE_PORT=COMx          (solo si la placa CAFÉ tiene serial al PC; NO COM3 lector)
  ATP gym: LOCK_CHAR=l (o h,i)  UNLOCK_CHAR=a (o b,c,d,e,f,g)  BAUD=19200
  TURNSTILE_UNLOCK_MS=8000          (tiempo libre para pasar empujando)
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
_relock_timer: threading.Timer | None = None
_active_ser = None  # mismo COM3 que serial_card_reader (placa cafe -> PC)


def set_active_serial(ser) -> None:
    """Usar el puerto ya abierto por el lector (evita 'acceso denegado' en COM3)."""
    global _active_ser
    _active_ser = ser


def clear_active_serial() -> None:
    global _active_ser
    _active_ser = None


def _load_gate_env_file() -> None:
    """Carga turnstile-gate.env (Sport Gym Acceso no hereda variables del .bat)."""
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
            if key and key not in os.environ:
                os.environ[key] = value


def _hex_bytes(name: str) -> bytes:
    raw = os.environ.get(name, "").replace(" ", "").strip()
    if not raw:
        return b""
    return bytes.fromhex(raw)


def _read_config() -> None:
    global MODE, GATE_PORT, GATE_BAUD, LOCK_BYTES, UNLOCK_BYTES, UNLOCK_MS, HTTP_LOCK, HTTP_UNLOCK
    global GATE_PROTOCOL, READER_BAUD
    _load_gate_env_file()
    MODE = os.environ.get("TURNSTILE_GATE_MODE", "none").lower().strip()
    GATE_PORT = os.environ.get("TURNSTILE_GATE_PORT", "").strip()
    if not GATE_PORT:
        GATE_PORT = os.environ.get("SERIAL_PORT", "").strip()
    GATE_PROTOCOL = os.environ.get("TURNSTILE_GATE_PROTOCOL", "").lower().strip()
    READER_BAUD = int(os.environ.get("SERIAL_BAUD", "9600"))
    GATE_BAUD = int(os.environ.get("TURNSTILE_GATE_BAUD", "9600"))

    if GATE_PROTOCOL in ("atp", "atp-acceso", "atp4", "atp-acceso-4"):
        GATE_BAUD = int(os.environ.get("TURNSTILE_GATE_BAUD", "19200"))
        lock_ch = os.environ.get("TURNSTILE_LOCK_CHAR", "d").strip() or "d"
        unlock_ch = os.environ.get("TURNSTILE_UNLOCK_CHAR", "h").strip() or "h"
        LOCK_BYTES = lock_ch.encode("ascii")[:1]
        UNLOCK_BYTES = unlock_ch.encode("ascii")[:1]
    else:
        LOCK_BYTES = _hex_bytes("TURNSTILE_LOCK_BYTES")
        UNLOCK_BYTES = _hex_bytes("TURNSTILE_UNLOCK_BYTES")
        if not LOCK_BYTES and os.environ.get("TURNSTILE_LOCK_CHAR", "").strip():
            LOCK_BYTES = os.environ.get("TURNSTILE_LOCK_CHAR", "d").encode("ascii")[:1]
        if not UNLOCK_BYTES and os.environ.get("TURNSTILE_UNLOCK_CHAR", "").strip():
            UNLOCK_BYTES = os.environ.get("TURNSTILE_UNLOCK_CHAR", "h").encode("ascii")[:1]

    UNLOCK_MS = int(os.environ.get("TURNSTILE_UNLOCK_MS", "8000"))
    HTTP_LOCK = os.environ.get("TURNSTILE_HTTP_LOCK", "").strip()
    HTTP_UNLOCK = os.environ.get("TURNSTILE_HTTP_UNLOCK", "").strip()


_read_config()
GATE_PROTOCOL = ""
READER_BAUD = 9600


def _payload_label(payload: bytes) -> str:
    if len(payload) == 1 and 32 <= payload[0] <= 126:
        return f"ascii '{chr(payload[0])}' hex={payload.hex()}"
    return payload.hex()


def _log(msg: str) -> None:
    print(f"[{time.strftime('%H:%M:%S')}] SEGURO: {msg}", flush=True)


def _send_serial(payload: bytes, label: str) -> bool:
    if not payload:
        return False
    if not GATE_PORT:
        _log(f"{label}: falta TURNSTILE_GATE_PORT (mismo que SERIAL_PORT/COM3 si cafe->PC)")
        return False
    if serial is None:
        _log("Instale pyserial: pip install pyserial")
        return False
    if _active_ser is not None and getattr(_active_ser, "is_open", False):
        try:
            old_baud = _active_ser.baudrate
            switched = GATE_BAUD != old_baud
            if switched:
                _active_ser.baudrate = GATE_BAUD
                time.sleep(0.1)
            _active_ser.write(payload)
            _active_ser.flush()
            time.sleep(0.15)
            if switched:
                _active_ser.baudrate = old_baud
            _log(f"{label} → {GATE_PORT} @ {GATE_BAUD} via lector ({_payload_label(payload)})")
            return True
        except serial.SerialException as ex:
            _log(f"{label} error en puerto compartido: {ex}")
            return False
    try:
        with serial.Serial(GATE_PORT, GATE_BAUD, timeout=1) as ser:
            ser.write(payload)
            ser.flush()
        _log(f"{label} → {GATE_PORT} @ {GATE_BAUD} ({_payload_label(payload)})")
        return True
    except serial.SerialException as ex:
        _log(f"{label} error en {GATE_PORT}: {ex}")
        _log("Si el lector ya usa COM3, deje solo iniciar-lector-tarjeta.bat abierto.")
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
            _log(f"{label} → {url} ({resp.status})")
            return True
    except Exception as ex:
        _log(f"{label} HTTP error {url}: {ex}")
        return False


def lock_gate() -> None:
    """Pone el seguro (torniquete bloqueado)."""
    global _relock_timer
    if _relock_timer:
        _relock_timer.cancel()
        _relock_timer = None
    if MODE == "serial":
        if LOCK_BYTES:
            _send_serial(LOCK_BYTES, "PONER seguro")
        elif UNLOCK_BYTES:
            _log("Configure TURNSTILE_LOCK_BYTES (comando para bloquear)")
        else:
            _log("Configure TURNSTILE_LOCK_BYTES y TURNSTILE_UNLOCK_BYTES")
        return
    if MODE == "http":
        _post_http(HTTP_LOCK, "PONER seguro")
        return
    _log("Modo none — ponga TURNSTILE_GATE_MODE=serial y LOCK/UNLOCK_BYTES en turnstile-gate.env")


def unlock_gate(ms: int | None = None) -> None:
    """Quita el seguro; tras ms vuelve a ponerlo."""
    global _relock_timer
    duration = UNLOCK_MS if ms is None else ms
    if MODE == "serial":
        if UNLOCK_BYTES:
            _send_serial(UNLOCK_BYTES, "QUITAR seguro")
        else:
            _log("Configure TURNSTILE_UNLOCK_BYTES")
            return
    elif MODE == "http":
        if not _post_http(HTTP_UNLOCK, "QUITAR seguro"):
            return
    else:
        _log("Modo none — no se puede quitar seguro")
        return

    if _relock_timer:
        _relock_timer.cancel()

    def _relock() -> None:
        lock_gate()

    _relock_timer = threading.Timer(max(1, duration) / 1000.0, _relock)
    _relock_timer.daemon = False
    _relock_timer.start()
    _log(f"Re-seguro en {duration} ms")


def queue_gate_command(cmd: str, data: dict | None = None) -> None:
    """Cola orden para el lector (Sport Gym Acceso usa COM3 sin abrirlo dos veces)."""
    payload = {"cmd": cmd, "data": data, "ts": time.time()}
    try:
        with open(_PENDING_FILE, "w", encoding="utf-8") as fh:
            json.dump(payload, fh)
    except OSError as ex:
        _log(f"No se pudo encolar {cmd}: {ex}")


def consume_pending_gate() -> bool:
    """Aplica una orden pendiente (llamar desde el bucle del lector)."""
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
        result = str(data.get("result", "")).upper()
        gate_opened = bool(data.get("gateOpened", False))
        if result == "SELECT_MEMBER":
            lock_gate()
        elif result == "GRANTED" and gate_opened:
            unlock_gate()
        else:
            lock_gate()
    else:
        _log(f"Orden pendiente desconocida: {cmd}")
    return True


def after_api_response(body: str) -> None:
    """Interpreta respuesta JSON del API y aplica seguro."""
    try:
        data = json.loads(body)
    except json.JSONDecodeError:
        lock_gate()
        return
    result = str(data.get("result", "")).upper()
    gate_opened = bool(data.get("gateOpened", False))
    if result == "SELECT_MEMBER":
        lock_gate()
        return
    if result == "GRANTED" and gate_opened:
        unlock_gate()
    else:
        lock_gate()


def main() -> None:
    _read_config()
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(0)
    cmd = sys.argv[1].lower()
    if cmd == "lock":
        lock_gate()
    elif cmd == "unlock":
        unlock_gate()
    elif cmd == "after-api" and len(sys.argv) > 2:
        after_api_response(sys.argv[2])
    else:
        print("Uso: turnstile_gate.py lock|unlock|after-api '{json}'")
        sys.exit(1)


if __name__ == "__main__":
    main()
