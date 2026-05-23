#!/usr/bin/env python3
"""
Puente lector RFID por USB-SERIAL (CH340 en COM3, etc.) → API Sport Gym.

Muchos lectores envían el número de tarjeta como texto por el puerto serie
al pasar la tarjeta. Este script reenvía cada lectura a /api/access/zkt/event.

Uso (Windows, PC de recepción):
  pip install pyserial
  set ACCESS_DEVICE_KEY=clave-torniquete-produccion-2026
  set GYM_ACCESS_API=https://sportgymr10.com/api/access/zkt/event
  set SERIAL_PORT=COM3
  set SERIAL_BAUD=9600
  python serial_card_reader.py

Prueba sin lector: python zkt_card_event.py NUMERO
"""
from __future__ import annotations

import atexit
import json
import os
import re
import signal
import sys
import time
import urllib.error
import urllib.request

try:
    import serial
except ImportError:
    print("Instala pyserial: pip install pyserial", file=sys.stderr)
    sys.exit(1)

API = os.environ.get("GYM_ACCESS_API", "http://localhost:8081/api/access/zkt/event")
KEY = os.environ.get("ACCESS_DEVICE_KEY", "gym-turnstile-dev-key")
PORT = os.environ.get("SERIAL_PORT", "COM3")
BAUD = int(os.environ.get("SERIAL_BAUD", "9600"))
DEBUG = os.environ.get("SERIAL_DEBUG", "").lower() in ("1", "true", "yes", "si", "sí")
# Tarjetas ZKT por COM suelen ser binario: hex=218AE2, decimal=2198114
PIN_FORMAT = os.environ.get("SERIAL_PIN_FORMAT", "hex").lower()
PID_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".lector-tarjeta.pid")


def release_port() -> None:
    try:
        os.remove(PID_FILE)
    except OSError:
        pass


def claim_port() -> None:
    with open(PID_FILE, "w", encoding="utf-8") as f:
        f.write(str(os.getpid()))


def stop_handler(signum=None, frame=None) -> None:
    release_port()
    print("\nLector detenido. Puerto serie liberado para otras aplicaciones.")
    raise SystemExit(0)


def normalize_card(raw: bytes) -> str | None:
    if not raw:
        return None
    text = raw.decode("utf-8", errors="ignore").strip()
    digits = re.sub(r"\D", "", text)
    # Texto claro: solo números (1061778723)
    if len(digits) >= 4:
        return digits
    # Panel ZKT binario: b'!\x8a\xe2' → 218AE2 (el 0x21 es '!' y antes se ignoraba)
    if 1 <= len(raw) <= 8:
        if PIN_FORMAT == "decimal":
            return str(int.from_bytes(raw, "big"))
        return raw.hex().upper()
    hex_only = re.sub(r"[^0-9A-Fa-f]", "", text)
    if len(hex_only) >= 8:
        return str(int(hex_only, 16))
    return text if len(text) >= 4 else None


_last_pin = ""
_last_at = 0.0


def handle_frame(data: bytes) -> None:
    global _last_pin, _last_at
    if not data:
        return
    if DEBUG:
        print(f"[RAW] {data!r} hex={data.hex()}")
    pin = normalize_card(data)
    if not pin:
        if DEBUG:
            print("  (no se pudo interpretar como tarjeta)")
        return
    now = time.monotonic()
    if pin == _last_pin and (now - _last_at) < 2.0:
        return
    _last_pin = pin
    _last_at = now
    post_pin(pin)


def post_pin(pin: str) -> None:
    body = json.dumps({"pin": pin}).encode()
    req = urllib.request.Request(
        API,
        data=body,
        headers={"Content-Type": "application/json", "X-Device-Key": KEY},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            body = resp.read().decode()
            print(f"[{time.strftime('%H:%M:%S')}] {pin} → {body}")
            try:
                from turnstile_gate import after_api_response

                after_api_response(body)
            except ImportError:
                pass
    except urllib.error.HTTPError as e:
        err_body = e.read().decode()
        print(f"[{time.strftime('%H:%M:%S')}] {pin} → HTTP {e.code}: {err_body}", file=sys.stderr)
        try:
            from turnstile_gate import lock_gate

            lock_gate()
        except ImportError:
            pass
    except Exception as ex:
        print(f"[{time.strftime('%H:%M:%S')}] {pin} → error: {ex}", file=sys.stderr)


def main() -> None:
    if not KEY or KEY == "gym-turnstile-dev-key":
        print("Define ACCESS_DEVICE_KEY (misma clave que deploy/.env)", file=sys.stderr)
    atexit.register(release_port)
    signal.signal(signal.SIGINT, stop_handler)
    if hasattr(signal, "SIGTERM"):
        signal.signal(signal.SIGTERM, stop_handler)
    claim_port()
    try:
        from turnstile_gate import clear_active_serial, lock_gate, set_active_serial
    except ImportError:
        clear_active_serial = lock_gate = set_active_serial = None  # type: ignore

    print(f"Escuchando {PORT} @ {BAUD} baud → {API}")
    if DEBUG:
        print("Modo DEBUG: muestra todo lo que llega por COM (SERIAL_DEBUG=1).")
    print("Pase una tarjeta. Ctrl+C o detener-lector-tarjeta.bat para liberar COM.")
    print("Formato Pin:", PIN_FORMAT, "(hex=218AE2, decimal=2198114)")
    try:
        with serial.Serial(PORT, BAUD, timeout=0.1) as ser:
            if set_active_serial:
                set_active_serial(ser)
                lock_gate()
            ser.reset_input_buffer()
            pending = bytearray()
            idle_since: float | None = None
            while True:
                if ser.in_waiting:
                    pending.extend(ser.read(ser.in_waiting))
                    idle_since = time.monotonic()
                elif pending and idle_since and (time.monotonic() - idle_since) > 0.08:
                    handle_frame(bytes(pending))
                    pending.clear()
                    idle_since = None
                else:
                    try:
                        from turnstile_gate import consume_pending_gate

                        consume_pending_gate()
                    except ImportError:
                        pass
                    time.sleep(0.03)
    except serial.SerialException as ex:
        release_port()
        print(f"No se pudo abrir {PORT}: {ex}", file=sys.stderr)
        print("Cierra ZKAccess u otra app que use el mismo puerto, o ejecuta detener-lector-tarjeta.bat.", file=sys.stderr)
        sys.exit(1)
    finally:
        try:
            from turnstile_gate import clear_active_serial, lock_gate

            lock_gate()
            clear_active_serial()
        except ImportError:
            pass
        release_port()


if __name__ == "__main__":
    main()
