#!/usr/bin/env python3
"""Simula una lectura de huella contra el API del gym (pruebas locales)."""
import json
import sys
import urllib.request

API = "http://localhost:8081/api/access/verify"
KEY = "gym-turnstile-dev-key"


def main():
    fp_id = sys.argv[1] if len(sys.argv) > 1 else input("ID de huella en el lector: ").strip()
    req = urllib.request.Request(
        API,
        data=json.dumps({"fingerprintUserId": fp_id}).encode(),
        headers={"Content-Type": "application/json", "X-Device-Key": KEY},
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        print(resp.read().decode())


if __name__ == "__main__":
    main()
