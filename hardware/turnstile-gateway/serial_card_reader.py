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
    text = raw.decode("utf-8", errors="ignore").strip()
    if not text:
        return None
    # Solo dígitos (ej. 1061778723)
    digits = re.sub(r"\D", "", text)
    if len(digits) >= 4:
        return digits
    # Hex sin separadores (ej. A1B2C3D4)
    hex_only = re.sub(r"[^0-9A-Fa-f]", "", text)
    if len(hex_only) >= 8:
        return str(int(hex_only, 16))
    return text if len(text) >= 4 else None


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
            print(f"[{time.strftime('%H:%M:%S')}] {pin} → {resp.read().decode()}")
    except urllib.error.HTTPError as e:
        print(f"[{time.strftime('%H:%M:%S')}] {pin} → HTTP {e.code}: {e.read().decode()}", file=sys.stderr)
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
    print(f"Escuchando {PORT} @ {BAUD} baud → {API}")
    print("Pase una tarjeta. Ctrl+C o detener-lector-tarjeta.bat para liberar COM.")
    try:
        with serial.Serial(PORT, BAUD, timeout=0.5) as ser:
            ser.reset_input_buffer()
            while True:
                chunk = ser.readline()
                if not chunk:
                    continue
                pin = normalize_card(chunk)
                if pin:
                    post_pin(pin)
    except serial.SerialException as ex:
        release_port()
        print(f"No se pudo abrir {PORT}: {ex}", file=sys.stderr)
        print("Cierra ZKAccess u otra app que use el mismo puerto, o ejecuta detener-lector-tarjeta.bat.", file=sys.stderr)
        sys.exit(1)
    finally:
        release_port()


if __name__ == "__main__":
    main()
