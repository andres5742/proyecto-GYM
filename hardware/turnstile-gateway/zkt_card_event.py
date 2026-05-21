#!/usr/bin/env python3
"""Simula un evento de tarjeta/Pin desde terminal ZKTeco contra el API del gym."""
import argparse
import json
import os
import urllib.parse
import urllib.request

API = os.environ.get("GYM_ACCESS_API", "http://localhost:8081/api/access/zkt/event")
KEY = os.environ.get("ACCESS_DEVICE_KEY", "gym-turnstile-dev-key")


def main():
    parser = argparse.ArgumentParser(description="Simular acceso por tarjeta ZKTeco")
    parser.add_argument("pin", nargs="?", help="Número Pin / tarjeta en el ZKTeco")
    parser.add_argument(
        "--form",
        action="store_true",
        help="Enviar como formulario (Pin=) como ADMS/Push",
    )
    args = parser.parse_args()

    pin = args.pin or input("Pin / número de tarjeta en ZKT: ").strip()
    if args.form:
        data = urllib.parse.urlencode({"Pin": pin}).encode()
        headers = {
            "Content-Type": "application/x-www-form-urlencoded",
            "X-Device-Key": KEY,
        }
    else:
        data = json.dumps({"pin": pin}).encode()
        headers = {"Content-Type": "application/json", "X-Device-Key": KEY}

    req = urllib.request.Request(API, data=data, headers=headers, method="POST")
    with urllib.request.urlopen(req) as resp:
        print(resp.read().decode())


if __name__ == "__main__":
    main()
