# Sport Gym — Paso a paso (despliegue + acceso completo)

Dominio de ejemplo: **https://sportgymr10.com**  
VPS: `/apps/gym-app` · Puerto Gym: **8081** (no usar 8080)

---

## A. Desplegar cambios en el servidor

### En tu PC (después de programar)

```bash
cd "/home/david/proyectos pagos/proyecto gym"
git add .
git commit -m "Descripción del cambio"
git push origin master
```

### En el VPS (SSH)

```bash
cd /apps/gym-app
git pull
docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d --build
```

### Comprobar

```bash
docker compose -f docker-compose.prod.yml ps
docker logs gym-backend --tail 30
curl -s https://sportgymr10.com/api/health
```

Portainer: http://72.61.65.92:9000/#!/auth

---

## B. Configuración de acceso — TODO (checklist maestro)

Marca cada ítem cuando esté listo.

### B.1 Servidor (una vez)

| ☐ | Tarea |
|---|--------|
| ☐ | Código desplegado (`git pull` + `docker compose up -d --build`) |
| ☐ | `deploy/.env` con `ACCESS_DEVICE_KEY=clave-larga-secreta` |
| ☐ | Rebuild frontend + backend tras cambiar la clave |
| ☐ | `curl` a `/api/access/zkt/event` responde JSON |
| ☐ | Módulo **Acceso / torniquete** activo (Superadmin → Módulos) |
| ☐ | (Opcional) `TURNSTILE_WEBHOOK=...` en `deploy/.env` para abrir puerta física |

Variables en **`/apps/gym-app/deploy/.env`**:

```env
ACCESS_DEVICE_KEY=TuClaveSecretaTorniquete2026
TURNSTILE_WEBHOOK=
FACE_MATCH_MAX_DISTANCE=0.55
```

Prueba API (debe responder JSON, aunque sea “no registrada”):

```bash
curl -s -X POST "https://sportgymr10.com/api/access/zkt/event" \
  -H "Content-Type: application/json" \
  -H "X-Device-Key: TuClaveSecretaTorniquete2026" \
  -d '{"pin":"99999"}'
```

### B.2 Hardware ZKT (lector KR de pared — tu equipo)

| ☐ | Tarea |
|---|--------|
| ☐ | Lector KR cableado al **panel ZKTeco** (Wiegand o RS485) |
| ☐ | Panel conectado al **PC de recepción** |
| ☐ | **ZKAccess** / **ZKBio Access** / **BioTime** instalado |
| ☐ | Lector dado de alta como **Reader** de la puerta en el software ZKT |
| ☐ | ZKT configurado para enviar eventos al gym (Push/ADMS o script puente) |

### B.3 Por cada afiliado

| ☐ | Tarea |
|---|--------|
| ☐ | Membresía **activa** en el gym |
| ☐ | (Tarjeta) Enrolada en ZKT + mismo número en gym → **Tarjeta ZKTeco** |
| ☐ | (Huella) ID de huella en ZKT + gym → **Huella en lector** |
| ☐ | (Rostro) Captura en panel → **Rostro con cámara** |
| ☐ | Prueba de ingreso aparece en **Acceso / torniquete → Ingresos** |

---

## C. Arquitectura (cómo encaja todo)

```text
┌─────────────────────────────────────────────────────────────┐
│  ENTRADA DEL GYM                                            │
├─────────────────────────────────────────────────────────────┤
│  1. Tarjeta / Pin  → Lector KR → Panel ZKT → PC ZKAccess   │
│                      → POST sportgymr10.com/api/access/zkt/event
│  2. Huella         → Mismo flujo (ID huella como Pin)      │
│  3. Rostro         → PC/tablet https://sportgymr10.com/acceso
│  4. Manual         → Panel Acceso / torniquete → Abrir      │
│  5. Facturación    → Pase día / bailes abre torniquete      │
└─────────────────────────────────────────────────────────────┘
                              ↓
                    Valida membresía + 1 ingreso/día
                              ↓
                    TURNSTILE_WEBHOOK (opcional) → relé/puerta
```

---

## D. Panel web del gym (todos los métodos)

Entra: **https://sportgymr10.com** → menú **Acceso / torniquete**

| Método | Dónde registrar | Dónde ingresa el afiliado |
|--------|-----------------|---------------------------|
| **Tarjeta ZKTeco** | Registrar → Tarjeta ZKTeco | Lector KR en la pared |
| **Huella** | Registrar → Huella en lector | Lector / terminal ZKT |
| **Rostro** | Registrar → Rostro con cámara | `https://sportgymr10.com/acceso` |
| **Manual** | Afiliados registrados → **Abrir** | Recepción autoriza |
| **Entrenadores** | Registrar → pestaña Entrenadores | Mismos métodos |

Reglas del sistema:

- Membresía debe estar **activa** (no vencida ni congelada).
- **Un ingreso por día** por afiliado (la apertura manual puede omitir esto).
- **Tiquetera:** cupo de entrenos del plan; al agotarlo no entra hasta renovar.

---

## E. Paso a paso DETALLADO — Lector de tarjetas (KR ZKTeco)

Guía completa: lector KR en la pared → panel ZKT → PC → servidor Sport Gym.

### E.0 Qué necesitas

| Pieza | Función |
|--------|---------|
| Lector **KR** (RFID, pared) | Lee la tarjeta |
| **Panel ZKTeco** | Recibe la lectura (Wiegand o RS485) |
| **PC recepción** (Windows) | ZKAccess / ZKBio Access / BioTime |
| **Internet** en el PC | Enviar eventos al servidor |
| Servidor gym desplegado | `https://sportgymr10.com` |
| Tablet/PC entrada (opcional) | `/acceso` — bienvenida en pantalla |

El KR **no** va al navegador ni al servidor directo. Ruta: **KR → panel ZKT → PC → API gym**.

---

### E.1 Cableado físico (una vez)

1. Apaga alimentación del panel ZKT.
2. Conecta el **KR** al panel (Wiegand 4 hilos o RS485, según manual).
3. Red del panel o cable al PC (según modelo).
4. Enciende. En ZKT: **Readers** → lector **Online**.
5. Pasa tarjeta de prueba: el software ZKT debe registrar lectura.

**Luces KR:** azul = espera · verde = leída · rojo = no enrolada en ZKT.

---

### E.2 Servidor del gym (una vez)

**1.** En el VPS, edita `/apps/gym-app/deploy/.env`:

```env
ACCESS_DEVICE_KEY=TuClaveSecretaTorniquete2026
TURNSTILE_WEBHOOK=
```

**2.** Despliega:

```bash
cd /apps/gym-app && git pull
docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d --build
```

**3.** Prueba API:

```bash
curl -s -X POST "https://sportgymr10.com/api/access/zkt/event" \
  -H "Content-Type: application/json" \
  -H "X-Device-Key: TuClaveSecretaTorniquete2026" \
  -d '{"pin":"99999"}'
```

Debe responder **JSON** (aunque sea “no reconocida”), no 401.

**4.** Superadmin → **Módulos** → activa **Acceso / torniquete**.

---

### E.3 Enrolar tarjeta en ZKTeco (cada afiliado)

En el **PC con ZKAccess**:

1. **Personal → Usuarios** → Agregar o editar.
2. Nombre del afiliado.
3. **Pin / ID de usuario:** puede ser la **cédula** (el gym la reconoce sin vincular tarjeta) o un número interno (ej. `10042`).
4. **Tarjeta / Card / Enrolar:** clic Enrolar → afiliado **pasa tarjeta** en el KR (luz verde).
5. **Guardar** en ZKT.
6. **Anotar** el **Pin** o **Card No** que muestra el software.
7. Probar otra vez en ZKT: debe salir el nombre en eventos.

Teclado en lector: Pin + `#`.

---

### E.4 Vincular en el panel Sport Gym

1. **https://sportgymr10.com** → **Acceso / torniquete** → **Registrar**.
2. **Afiliados** → **Tarjeta ZKTeco**.
3. Busca afiliado.
4. Pega el **mismo número** del ZKT (sin espacios) → **Vincular tarjeta**.

**Atajo con cédula:** si el Pin en ZKT = cédula del afiliado y está **activo** en el gym, no hace falta vincular tarjeta en el panel.

---

### E.5 Conectar ZKT al servidor (obligatorio)

Sin esto el gym **no** recibe el ingreso.

| Dato | Valor |
|------|--------|
| URL | `https://sportgymr10.com/api/access/zkt/event` |
| Método | POST |
| Cabecera | `X-Device-Key: TuClaveSecretaTorniquete2026` |
| JSON | `{"pin":"10042"}` |
| Form ADMS | `Pin=10042` |

**Opción A — Push / ADMS en ZKT**

1. ZKAccess → **Sistema → Opciones** / **Comunicación**.
2. Activa **ADMS** / **Push** / **Web Server**.
3. URL: `https://sportgymr10.com/api/access/zkt/event`
4. Cabecera `X-Device-Key` = tu clave (si el firmware lo permite).
5. Guardar, reiniciar servicio ZKT si aplica.
6. Pasa tarjeta → revisa **Acceso → Ingresos** en el gym.

**Opción B — Script puente** (si ZKT no envía cabeceras)

En el PC de recepción:

```bash
cd hardware/turnstile-gateway
export ACCESS_DEVICE_KEY=TuClaveSecretaTorniquete2026
export GYM_ACCESS_API=https://sportgymr10.com/api/access/zkt/event
python3 zkt_card_event.py 10042
```

Configura ZKT para **ejecutar ese script** en cada evento, pasando el Pin (ej. `zkt_card_event.py %PIN%` en Windows).

---

### E.6 Pantalla `/acceso`

1. Tablet/PC en entrada: `https://sportgymr10.com/acceso` (se activa sola).
2. Al pasar tarjeta (con E.5 OK) → bienvenida + voz.
3. Prueba manual: campo **Cédula** + **OK** abajo.

Rebuild frontend si cambiaste `ACCESS_DEVICE_KEY`.

---

### E.7 Prueba completa

| # | Acción | OK si… |
|---|--------|--------|
| 1 | curl E.2 | JSON 200 |
| 2 | Membresía activa en gym | — |
| 3 | Tarjeta en ZKT | Luz verde |
| 4 | Mismo Pin en gym o Pin = cédula | Vinculado / activo |
| 5 | Push o script | Aparece en **Ingresos** |
| 6 | `/acceso` abierto | Bienvenida al pasar tarjeta |

```bash
python3 hardware/turnstile-gateway/zkt_card_event.py NUMERO_PIN
```

---

### E.8 Errores frecuentes (tarjeta)

| Síntoma | Solución |
|---------|----------|
| Luz roja KR | Enrolar tarjeta en ZKT (E.3) |
| No reconocida | Mismo número E.4 o Pin = cédula |
| No aparece en Ingresos | Configurar E.5 |
| Dispositivo no autorizado | `ACCESS_DEVICE_KEY` + rebuild |
| Membresía vencida | Renovar plan |
| Ya ingresó hoy | 1 ingreso/día; manual (H) |
| Puerta no abre | `TURNSTILE_WEBHOOK` (sección J) |

---

### E.9 Orden primera vez

1. E.2 Servidor  
2. E.1 Cableado  
3. E.3 Una tarjeta en ZKT  
4. E.4 Vincular en gym  
5. E.5 Push o script  
6. E.7 Probar tarjeta + `/acceso`  
7. Repetir E.3–E.4 para todos  

---

## F. Paso a paso — Huella (ZKTeco)

### F.1 En ZKT

Enrola huella del usuario en ZKT. El **ID de usuario (Pin)** puede ser:

- El **número de cédula** del afiliado (recomendado): el sistema lo reconoce si el afiliado existe y la membresía está **activa**, sin vincular huella en el panel.
- O un ID interno (ej. `20015`): entonces debes vincularlo en el panel (F.2).

### F.2 En el gym (solo si usas ID interno, no cédula)

**Registrar** → **Huella en lector** → mismo ID → **Vincular huella**.

### F.3 Evento al servidor

Mismo que tarjeta: el ZKT debe enviar ese ID a:

`POST https://sportgymr10.com/api/access/zkt/event` con el Pin/ID.

O bien:

```bash
curl -s -X POST "https://sportgymr10.com/api/access/verify" \
  -H "Content-Type: application/json" \
  -H "X-Device-Key: TuClaveSecretaTorniquete2026" \
  -d '{"deviceUserId":"20015","credentialType":"FINGERPRINT"}'
```

Prueba local huella (cédula o ID):

```bash
python3 hardware/turnstile-gateway/zkt_card_event.py 1234567890
# o simulate_scan.py con el mismo Pin
```

---

## G. Pantalla de ingreso `/acceso` (tarjeta + huella, sin cámara)

1. PC/tablet en la entrada: **https://sportgymr10.com/acceso** (se activa sola al cargar).
2. Deja la pantalla abierta: muestra *Pase su tarjeta o huella*.
3. El afiliado pasa tarjeta o huella en el **ZKTeco** → el software ZKT llama al API → la pantalla muestra la bienvenida y la voz.
4. Si el navegador bloquea la voz, un toque en la pantalla o el botón **Escuchar bienvenida** la activa.

**Requisito:** ZKTeco configurado (sección E.3) enviando eventos a `/api/access/zkt/event`.

**Prueba sin lector:** en `/acceso` → campo **Cédula** + **OK**; o `python3 zkt_card_event.py PIN`.

### Rostro (cámara) — desactivado en kiosk por ahora

Sigue disponible el registro en **Acceso / torniquete → Rostro con cámara** para uso futuro.

---

## H. Paso a paso — Apertura manual (recepción)

1. **Acceso / torniquete** → **Afiliados registrados**.
2. En la tabla de huellas (o tarjetas) → botón **Abrir** del afiliado.
3. Registra ingreso y puede abrir torniquete (si hay `TURNSTILE_WEBHOOK`).

Útil si olvidó tarjeta o hay problema con el lector.

---

## I. Paso a paso — Entrenadores

1. **Acceso / torniquete** → **Registrar** → pestaña **Entrenadores**.
2. Elige método: huella, tarjeta o rostro.
3. Mismo flujo que afiliados; el sistema valida que el empleado esté activo.

---

## J. Torniquete / puerta física (opcional)

Cuando el ingreso es autorizado (`gateOpened: true`), el backend llama:

```env
TURNSTILE_WEBHOOK=http://IP_O_HOST_DEL_RELE:PUERTO/ruta
```

Necesitas un servicio o relé que reciba ese POST y active el motor.

Sin `TURNSTILE_WEBHOOK`: el gym **sí registra** el ingreso pero la puerta no se mueve sola.

---

## K. Errores frecuentes

| Mensaje / síntoma | Causa | Solución |
|-------------------|--------|----------|
| Tarjeta no registrada | Número no vinculado en gym | Sección E.2 |
| Luz roja en KR | Tarjeta no enrolada en ZKT | Sección E.1 |
| Ingreso no aparece en panel | ZKT no llama al API | Sección E.3 |
| Membresía vencida | Plan vencido | Facturación / renovar |
| Membresía congelada | Congelación activa | Descongelar en recepción |
| Ya ingresó hoy | Regla 1 ingreso/día | Normal o apertura manual |
| Dispositivo no autorizado | `ACCESS_DEVICE_KEY` incorrecta | Revisar `.env` y rebuild |
| Rostro no reconoce | No registrado o mala luz | Re-registrar en G.1 |

---

## L. Referencia API (técnicos)

| Uso | Endpoint | Cabecera |
|-----|----------|----------|
| Tarjeta/Pin ZKT | `POST /api/access/zkt/event` | `X-Device-Key` |
| Huella genérica | `POST /api/access/verify` | `X-Device-Key` |
| Rostro | `POST /api/access/webcam/verify` | `X-Device-Key` |
| Registrar (panel) | `POST /api/access/enroll` | JWT login |
| Manual | `POST /api/access/manual-open/{id}` | JWT login |

Cuerpo tarjeta ZKT: `{"pin":"10042"}`  
Cuerpo huella: `{"deviceUserId":"10042","credentialType":"FINGERPRINT"}`  
Cuerpo tarjeta verify: `{"deviceUserId":"10042","credentialType":"CARD"}`

---

## M. Orden recomendado la primera vez

1. **A** — Desplegar en VPS  
2. **B.1** — `ACCESS_DEVICE_KEY` + módulo ACCESO + curl prueba  
3. **B.2** — Cablear ZKT + software en PC  
4. **E** — Una tarjeta de prueba (ZKT + gym + Push/puente)  
5. **G** — Pantalla `/acceso` + un rostro de prueba  
6. **F** — Huella (si aplica)  
7. **J** — Relé/torniquete (si aplica)  
8. Enrolar al resto de afiliados (**B.3**)

**Documentación extra:** `hardware/turnstile-gateway/README.md` · `docs/GUIA-PRODUCCION-PASO-A-PASO.md`
