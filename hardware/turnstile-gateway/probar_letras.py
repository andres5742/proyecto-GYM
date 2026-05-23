#!/usr/bin/env python3
"""Prueba cada letra ATP en COM3 @ 19200. Anote cual mueve rele o permite pasar."""
import os
import sys
import time

try:
    import serial
except ImportError:
    print("pip install pyserial")
    sys.exit(1)

PORT = os.environ.get("TURNSTILE_GATE_PORT", "COM3")
BAUD = int(os.environ.get("TURNSTILE_GATE_BAUD", "19200"))
LETTERS = list("abcdefghil")

def main() -> None:
    print(f"Puerto {PORT} @ {BAUD}")
    print("Letras:", " ".join(LETTERS))
    print("BLOQUEAN (probado gym): b c d e  |  RESTO: pruebe desbloqueo\n")
    for ch in LETTERS:
        input(f"Enter = enviar '{ch}' ...")
        try:
            with serial.Serial(PORT, BAUD, timeout=1) as ser:
                ser.write(ch.encode("ascii"))
                ser.flush()
            print(f"  OK enviado '{ch}'\n")
        except serial.SerialException as ex:
            print(f"  ERROR: {ex}")
            sys.exit(1)
        time.sleep(2)
    print("Fin. Anote que letra movio el rele.")


if __name__ == "__main__":
    main()
