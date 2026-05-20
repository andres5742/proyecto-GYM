# Torniquete + acceso biométrico

El backend valida membresía y abre el torniquete. Soporta:

- **Huella** — lector ZKTeco / Suprema (ID de usuario en el dispositivo)
- **Rostro (biométrico)** — PC con cámara en `/acceso` (reconocimiento en el navegador)

## Lector biométrico de rostro (cámara en la entrada)

1. En el panel **Acceso / torniquete**, sección *Lector biométrico de rostro*: elige afiliado → *Capturar y guardar en lector biométrico*.
2. En la entrada, abre `https://TU-DOMINIO/acceso` → pestaña **Rostro (biométrico)**.
3. El navegador pedirá permiso de cámara. Con buena luz, el ingreso es automático.

Ajuste de sensibilidad en el servidor: `FACE_MATCH_MAX_DISTANCE` (por defecto `0.55`, menor = más estricto).

## Huella (ID en dispositivo)

```http
POST /api/access/verify
X-Device-Key: TU_CLAVE
{"deviceUserId": "12345", "credentialType": "FINGERPRINT"}
```

## Rostro biométrico (descriptor desde el torniquete)

```http
POST /api/access/webcam/verify
X-Device-Key: TU_CLAVE
{"descriptor": [128 números]}
```

El descriptor lo genera el frontend con face-api; no envíes fotos al servidor.

## Relé del torniquete

`TURNSTILE_WEBHOOK` → URL local que active el relé cuando `"gateOpened": true`.

## Pruebas locales

```bash
python3 simulate_scan.py 12345
```

Rostro: usar la UI en `http://localhost:4201/acceso` → pestaña **Rostro (biométrico)**.
