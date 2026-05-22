#!/usr/bin/env python3
"""
Sin manual del instalador: escucha un puerto COM mientras usted abre/cierra el torniquete
con ZKAccess, el lector de pared o el programa que ya funcionaba.

Uso:
  set SNIFF_PORT=COM3
  python sniff_gate_port.py

Mientras corre (60 s): haga que el torniquete PONGA y QUITE el seguro.
Si aparece RECIBIDO o ENVIADO, anote hex para turnstile-gate.env.
"""
from __future__ import annotations

import os
import sys
import time

try:
    import serial
    from serial.tools import list_ports
except ImportError:
    print("pip install pyserial", file=sys.stderr)
    sys.exit(1)

PORT = os.environ.get("SNIFF_PORT", "COM3").strip()
BAUD = int(os.environ.get("SNIFF_BAUD", "9600"))
SECONDS = int(os.environ.get("SNIFF_SECONDS", "60"))


def main() -> None:
    print("=" * 55)
    print(" ESCUCHA PUERTO TORNiquete (sin manual)")
    print("=" * 55)
    print("\nPuertos COM detectados:")
    for p in list_ports.comports():
        mark = " <-- escuchando" if p.device.upper() == PORT.upper() else ""
        print(f"  {p.device}: {p.description}{mark}")
    print(f"\nPuerto: {PORT} @ {BAUD} — {SECONDS} segundos")
    print("Cierre Sport Gym lector si usa el mismo COM.")
    print("Abra ZKAccess o pase tarjeta en PARED para mover el seguro.\n")
    try:
        with serial.Serial(PORT, BAUD, timeout=0.1) as ser:
            deadline = time.monotonic() + SECONDS
            while time.monotonic() < deadline:
                if ser.in_waiting:
                    data = ser.read(ser.in_waiting)
                    print(f"  RECIBIDO: hex={data.hex()}  raw={data!r}")
                time.sleep(0.05)
    except serial.SerialException as ex:
        print(f"ERROR: {ex}")
        print("Pruebe otro COM en SNIFF_PORT o cierre la app que lo usa.")
        sys.exit(1)
    print("\nSi no salio nada:")
    print("  - ZKAccess suele abrir el seguro DENTRO de la placa cafe (lector pared),")
    print("    sin enviar bytes al PC. Eso es NORMAL.")
    print("  - COM3 en su gym trae el NUMERO de tarjeta; el rele puede no avisar por COM3.")
    print("  - Pruebe: probar-seguro-com3.bat (manda bytes de prueba al rele)")
    print("  - O repita escucha con SNIFF_PORT=COM4 si aparece otro COM en la lista.")


if __name__ == "__main__":
    main()
