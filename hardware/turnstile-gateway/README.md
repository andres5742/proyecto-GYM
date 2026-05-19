# Torniquete + lector de huellas

El backend expone `POST /api/access/verify` con el ID de huella que devuelve el lector (ZKTeco, Suprema, etc.).

## Configuración

1. En el lector, registra cada afiliado y anota su **ID de usuario** en el dispositivo.
2. En el panel **Acceso / torniquete**, vincula ese ID con el afiliado del sistema.
3. Configura el lector o un mini PC (Raspberry Pi) para que al validar huella llame:

```http
POST http://TU-SERVIDOR:8081/api/access/verify
X-Device-Key: gym-turnstile-dev-key
Content-Type: application/json

{"fingerprintUserId": "12345"}
```

4. Si la respuesta tiene `"result": "GRANTED"` y `"gateOpened": true`, activa el relé del torniquete.

## Relé del torniquete (opcional)

Define `TURNSTILE_WEBHOOK` en el backend apuntando a un script local, por ejemplo:

`http://192.168.1.50:9090/open`

Un script en la Raspberry puede escuchar ese puerto y activar GPIO.

## Prueba sin hardware

Usa la pantalla pública `/acceso` o el panel admin para simular una lectura de huella.
