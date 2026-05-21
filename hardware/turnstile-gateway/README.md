# Torniquete + acceso biométrico

El backend valida membresía y abre el torniquete. Soporta:

- **Huella** — lector ZKTeco / Suprema (ID de usuario en el dispositivo)
- **Tarjeta** — lector ZKTeco con tarjeta RFID (número Pin / Card en el dispositivo)
- **Rostro (biométrico)** — PC con cámara en `/acceso` (reconocimiento en el navegador)

## Registrar tarjeta ZKTeco (panel)

1. **Acceso / torniquete** → Registrar → **Tarjeta ZKTeco**.
2. Pasa la tarjeta en el lector y anota el **Pin** o número que muestra el software del ZKT (o el ID de usuario asignado en el terminal).
3. Vincula ese número al afiliado o entrenador.

Un afiliado puede tener huella, tarjeta y rostro a la vez (cada uno con su propio ID en el sistema).

## Lector biométrico de rostro (cámara en la entrada)

1. En el panel **Acceso / torniquete**, sección *Lector biométrico de rostro*: elige afiliado → *Capturar y guardar en lector biométrico*.
2. En la entrada, abre `https://TU-DOMINIO/acceso` → la cámara identifica automáticamente.
3. El navegador pedirá permiso de cámara. Con buena luz, el ingreso es automático.

Ajuste de sensibilidad en el servidor: `FACE_MATCH_MAX_DISTANCE` (por defecto `0.55`, menor = más estricto).

## Huella (ID en dispositivo)

```http
POST /api/access/verify
X-Device-Key: TU_CLAVE
{"deviceUserId": "12345", "credentialType": "FINGERPRINT"}
```

## Tarjeta ZKTeco (evento Pin / Card)

Cuando el terminal lee una tarjeta, debe notificar al servidor. Opciones:

### A) Endpoint dedicado ZKT (recomendado)

```http
POST /api/access/zkt/event
X-Device-Key: TU_CLAVE
Content-Type: application/json

{"pin": "12345"}
```

También acepta formulario ADMS/Push: `Pin=12345` con `Content-Type: application/x-www-form-urlencoded`.

Si el afiliado está registrado como **tarjeta**, se busca primero por `CARD`; si no, por `FINGERPRINT` (mismo número).

### B) Verify genérico

```http
POST /api/access/verify
X-Device-Key: TU_CLAVE
{"deviceUserId": "12345", "credentialType": "CARD"}
```

### Configurar el ZKTeco

En el software del terminal (ZKAccess / BioTime / ADMS):

1. Activa **Push** o **Web Server** hacia tu servidor.
2. URL de eventos de acceso: `https://TU-DOMINIO/api/access/zkt/event`
3. Cabecera personalizada (si el firmware lo permite): `X-Device-Key: TU_CLAVE`
4. El campo **Pin** del evento debe coincidir con el número vinculado en el panel.

Si el ZKT no permite cabeceras HTTP, usa un **puente local** (script en esta carpeta) que reciba el evento y reenvíe con la clave.

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
# Huella
python3 simulate_scan.py 12345

# Tarjeta (JSON)
python3 zkt_card_event.py 12345

# Tarjeta (formulario Pin= como ADMS)
python3 zkt_card_event.py 12345 --form
```

Rostro: usar la UI en `http://localhost:4201/acceso`.

Variables opcionales:

- `GYM_ACCESS_API` — URL del endpoint (por defecto `http://localhost:8081/api/access/zkt/event`)
- `ACCESS_DEVICE_KEY` — misma clave que en el backend (`app.access.device-api-key`)
