#!/usr/bin/env python3
"""Simula lectura de huella contra el API del gym."""
import argparse
import json
import sys
import urllib.request

API = "http://localhost:8081/api/access/verify"
KEY = "gym-turnstile-dev-key"


def main():
    parser = argparse.ArgumentParser(description="Simular acceso por huella")
    parser.add_argument("device_user_id", nargs="?", help="ID en el lector de huella")
    args = parser.parse_args()

    device_id = args.device_user_id or input("ID en el lector de huella: ").strip()
    body = {"deviceUserId": device_id, "credentialType": "FINGERPRINT"}
    req = urllib.request.Request(
        API,
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json", "X-Device-Key": KEY},
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        print(resp.read().decode())


if __name__ == "__main__":
    main()
