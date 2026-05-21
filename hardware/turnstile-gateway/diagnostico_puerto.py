#!/usr/bin/env python3
"""
Diagnóstico: lista puertos COM y muestra TODO lo que envía el lector (15 s por velocidad).
Ejecutar: probar-diagnostico-com.bat
"""
from __future__ import annotations

import os
import sys
import time

try:
    import serial
    from serial.tools import list_ports
except ImportError:
    print("Instala pyserial: pip install pyserial", file=sys.stderr)
    sys.exit(1)

PORT = os.environ.get("SERIAL_PORT", "COM3")
BAUDS = [int(b) for b in os.environ.get("SERIAL_BAUDS", "9600,115200,57600,19200").split(",") if b.strip()]
SECONDS = int(os.environ.get("SERIAL_TEST_SECONDS", "15"))


def main() -> None:
    print("=" * 50)
    print(" DIAGNOSTICO LECTOR - Sport Gym")
    print("=" * 50)
    print("\nPuertos COM en este PC:")
    found = list(list_ports.comports())
    if not found:
        print("  (ninguno detectado - ¿lector USB conectado?)")
    for p in found:
        mark = " <-- CONFIGURADO" if p.device.upper() == PORT.upper() else ""
        print(f"  {p.device}: {p.description}{mark}")

    print(f"\nProbando puerto: {PORT}")
    print(f"Pase la tarjeta varias veces en cada prueba ({SECONDS}s cada una).\n")

    any_data = False
    for baud in BAUDS:
        print(f"--- {PORT} @ {baud} baud ({SECONDS}s) ---")
        try:
            with serial.Serial(PORT, baud, timeout=0.1) as ser:
                ser.reset_input_buffer()
                deadline = time.monotonic() + SECONDS
                while time.monotonic() < deadline:
                    if ser.in_waiting:
                        data = ser.read(ser.in_waiting)
                        print(f"  RECIBIDO: {data!r}  (hex: {data.hex()})")
                        any_data = True
                    time.sleep(0.05)
                if not any_data:
                    print("  (sin datos en este intervalo)")
        except serial.SerialException as ex:
            print(f"  ERROR al abrir: {ex}")
        print()

    print("=" * 50)
    if any_data:
        print("OK: El lector SI envia datos por este puerto.")
        print("Use esa velocidad en iniciar-lector-tarjeta.bat:")
        print("  set SERIAL_BAUD=XXXX")
        print("Si el numero sale en RECIBIDO, el lector funciona con el script.")
    else:
        print("PROBLEMA: No llego NINGUN byte en ninguna prueba.")
        print("\nPosibles causas:")
        print("  1. Puerto incorrecto (no es COM3) -> mire la lista arriba")
        print("  2. El lector NO envia por USB-serial (solo ZKAccess / otro programa)")
        print("  3. Otra app tiene el puerto (cierre ZKAccess, ejecute detener-lector-tarjeta.bat)")
        print("  4. Cable USB solo alimenta; la lectura va por otro cable al panel ZKT")
    print("=" * 50)


if __name__ == "__main__":
    main()
