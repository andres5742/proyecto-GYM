# Guía paso a paso — Gym en producción (sin afectar RBM)

Esta guía asume tu servidor actual:

- **RBM / app Java** → puerto **8080** (`/opt/spring-angular-app`) — **no se toca**
- **Nginx** → puertos **80** y **443**
- **Portainer** → puerto **9000** (panel Docker)
- **IP del VPS** → ejemplo `72.61.65.92` (cambia por la tuya)

Al final tendrás:

```
https://gym.TUDOMINIO.com          → página web (Angular)
https://api.gym.TUDOMINIO.com      → API (Spring Boot + JWT)
```

RBM sigue en su dominio actual → `8080`.

---

## Fase 0 — Qué necesitas antes de empezar

### En Hostinger (o tu registrador)

- [ ] Dominio apuntando al VPS (ej. `tudominio.com`)
- [ ] Crear registros DNS (más adelante):
  - `gym` → IP del VPS
  - `api.gym` → IP del VPS

### En tu PC

- [ ] Código del gym en GitHub (repo `proyecto-GYM`)
- [ ] Cliente SSH al VPS (`ssh root@TU_IP`)

### Decidir dominios (ejemplo)

| Uso | Subdominio ejemplo |
|-----|-------------------|
| Web Gym | `gym.tudominio.com` |
| API Gym | `api.gym.tudominio.com` |
| RBM (ya existe) | `app.tudominio.com` o el que ya uses |

Anota tus dominios reales y úsalos en todos los pasos.

---

## Fase 1 — Portainer: conectar Docker (5 min)

1. Abre en el navegador: `http://TU_IP:9000`
2. Crea usuario administrador (si es primera vez).
3. En el asistente elige:
   - **Docker Standalone**
   - **Connect via socket** / **Local**
4. Nombre del entorno: `local` → **Connect**.

**Comprobación:** en **Home** debe aparecer `local` en estado **Up**.

Si falla, en SSH:

```bash
sudo systemctl start docker
sudo systemctl enable docker
docker ps
```

Recrear Portainer con socket (solo si hace falta):

```bash
docker stop portainer 2>/dev/null; docker rm portainer 2>/dev/null
docker volume create portainer_data
docker run -d -p 9000:9000 --name portainer --restart=always \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v portainer_data:/data \
  portainer/portainer-ce:latest
```

> Portainer es para **ver** contenedores. El primer despliegue del gym lo haremos por **terminal** (más fiable para compilar).

---

## Fase 2 — Preparar carpetas en el servidor (10 min)

Conéctate por SSH:

```bash
ssh root@TU_IP
```

### 2.1 Verificar que RBM sigue bien (no lo modifiques)

```bash
sudo ss -tulpn | grep 8080
ps aux | grep java
```

Debes ver Java escuchando en **8080**.

### 2.2 Crear estructura

```bash
sudo mkdir -p /apps/gym-app /backups /logs
sudo chown -R $USER:$USER /apps /backups /logs
```

### 2.3 Swap (recomendado para Java + Docker)

```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

### 2.4 Instalar Git (si no está)

```bash
sudo apt update
sudo apt install -y git docker-compose-plugin
```

---

## Fase 3 — Subir el código del Gym al servidor (10 min)

```bash
cd /apps/gym-app
git clone https://github.com/andres5742/proyecto-GYM.git .
```

Si el repo es privado, usa token SSH o sube con `rsync` desde tu PC:

```bash
# Desde TU PC (no en el servidor):
rsync -avz --exclude node_modules --exclude target \
  "/home/david/proyectos pagos/proyecto gym/" \
  root@TU_IP:/apps/gym-app/
```

Comprueba que existan:

```bash
ls /apps/gym-app/docker-compose.prod.yml
ls /apps/gym-app/backend/Dockerfile
ls /apps/gym-app/frontend/Dockerfile
```

---

## Fase 4 — Configurar variables de producción (15 min)

```bash
cd /apps/gym-app
cp deploy/.env.example deploy/.env
nano deploy/.env
```

**Ejemplo** (cambia `tudominio.com` por el tuyo):

```env
POSTGRES_DB=gym_db
POSTGRES_USER=gym_user
POSTGRES_PASSWORD=UnaClaveMuySegura123!

JWT_SECRET=genera-aqui-minimo-32-caracteres-aleatorios-xyz
JWT_EXPIRATION_MS=86400000
ACCESS_DEVICE_KEY=clave-secreta-torniquete-gym

CORS_ORIGINS=https://gym.tudominio.com

API_URL=https://api.gym.tudominio.com/api
UPLOADS_BASE_URL=https://api.gym.tudominio.com
```

Generar JWT_SECRET en el servidor:

```bash
openssl rand -base64 48
```

Guarda `deploy/.env` — **nunca** lo subas a GitHub.

---

## Fase 5 — Construir y levantar Gym con Docker (20–40 min)

La primera vez **compila** backend y frontend; puede tardar.

```bash
cd /apps/gym-app
docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d --build
```

Ver progreso:

```bash
docker compose -f docker-compose.prod.yml ps
docker logs -f gym-backend
# Ctrl+C para salir de logs
```

### Comprobaciones en el servidor

```bash
# API
curl -s http://127.0.0.1:8081/api/health

# Frontend (debe devolver 200)
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://127.0.0.1:3001/
```

En **Portainer** → **Containers** debes ver:

- `gym-postgres` — running
- `gym-backend` — running
- `gym-frontend` — running

**Puertos usados (solo localhost):**

| Servicio | Puerto host |
|----------|-------------|
| Backend | 8081 |
| Frontend | 3001 |
| RBM (intacto) | 8080 |

---

## Fase 6 — DNS en Hostinger (5 min)

En el panel DNS del dominio:

| Tipo | Nombre | Valor |
|------|--------|-------|
| A | `gym` | IP de tu VPS |
| A | `api.gym` | IP de tu VPS |

Espera 5–30 minutos (a veces hasta 1 h).

Comprueba:

```bash
ping gym.tudominio.com
ping api.gym.tudominio.com
```

---

## Fase 7 — Nginx: exponer Gym con HTTPS (20 min)

**No reemplaces** la config de RBM. Solo **añade** un archivo nuevo.

### 7.1 Ver cómo está configurado RBM hoy

```bash
sudo nginx -T | less
# Busca server_name y ssl_certificate de tu app actual
```

Anota las rutas de certificados SSL si usas Let's Encrypt, por ejemplo:

- `/etc/letsencrypt/live/tudominio.com/fullchain.pem`
- `/etc/letsencrypt/live/tudominio.com/privkey.pem`

### 7.2 Crear config del Gym

```bash
sudo nano /etc/nginx/sites-available/gym
```

Pega (ajusta dominios y rutas SSL):

```nginx
# Redirigir HTTP a HTTPS
server {
    listen 80;
    server_name gym.tudominio.com api.gym.tudominio.com;
    return 301 https://$host$request_uri;
}

# Frontend Angular
server {
    listen 443 ssl http2;
    server_name gym.tudominio.com;

    ssl_certificate     /etc/letsencrypt/live/tudominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/tudominio.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:3001;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# API Spring Boot
server {
    listen 443 ssl http2;
    server_name api.gym.tudominio.com;

    ssl_certificate     /etc/letsencrypt/live/tudominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/tudominio.com/privkey.pem;

    client_max_body_size 10M;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Activar:

```bash
sudo ln -sf /etc/nginx/sites-available/gym /etc/nginx/sites-enabled/gym
sudo nginx -t
sudo systemctl reload nginx
```

### 7.3 SSL con Certbot (si aún no tienes certificado para subdominios)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d gym.tudominio.com -d api.gym.tudominio.com
```

---

## Fase 8 — Pruebas finales en producción (10 min)

En tu navegador:

1. `https://gym.tudominio.com` — debe cargar la web del gym.
2. `https://api.gym.tudominio.com/api/health` — debe responder JSON OK.
3. Prueba login staff (superadmin del seed) o afiliado según tu `DataInitializer`.
4. Confirma que **RBM** sigue en su URL habitual.

En SSH si algo falla:

```bash
docker logs gym-backend --tail 100
docker logs gym-frontend --tail 50
sudo tail -50 /var/log/nginx/error.log
```

---

## Fase 9 — Datos iniciales y usuarios

El backend en primer arranque puede crear datos demo (`DataInitializer`) si la BD está vacía.

- **Staff:** usuario `superadmin` / contraseña según tu seed (revisa `DataInitializer.java` en el repo).
- **Afiliado:** documento como usuario, contraseña inicial = documento (si está configurado el portal).

**En producción:** cambia contraseñas demo y define `JWT_SECRET` fuerte.

---

## Fase 10 — Actualizar el Gym después de cambios en código

En el servidor:

```bash
cd /apps/gym-app
git pull
docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d --build
```

Si cambias dominios o `API_URL`, reconstruye frontend:

```bash
docker compose -f docker-compose.prod.yml --env-file deploy/.env build --no-cache frontend
docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d frontend
```

---

## Fase 11 — Backup de la base de datos

```bash
docker exec gym-postgres pg_dump -U gym_user gym_db > /backups/gym_$(date +%F).sql
```

Programar cron (opcional):

```bash
crontab -e
# Diario a las 3am:
# 0 3 * * * docker exec gym-postgres pg_dump -U gym_user gym_db > /backups/gym_$(date +\%F).sql
```

---

## Resumen visual

```
                    INTERNET
                        │
                   Nginx :443
                        │
        ┌───────────────┼───────────────┐
        │               │               │
   RBM dominio      gym.dominio    api.gym.dominio
        │               │               │
        ▼               ▼               ▼
    :8080 Java      :3001 Docker    :8081 Docker
   (sin cambios)   gym-frontend   gym-backend
                                      │
                                 gym-postgres
                                 (red interna)
```

---

## Errores frecuentes

| Problema | Qué revisar |
|----------|-------------|
| Portainer sin entornos | Conectar socket Docker (Fase 1) |
| `502 Bad Gateway` en Nginx | `docker ps`, `curl 127.0.0.1:8081` y `:3001` |
| CORS en login | `CORS_ORIGINS` en `deploy/.env` = URL exacta del frontend |
| API con localhost en el navegador | Rebuild frontend con `API_URL` correcto en `.env` |
| Puerto 8080 ocupado | Gym **no** debe usar 8080; solo 8081 |
| RBM dejó de funcionar | Revisaste config Nginx de RBM; no borres `sites-enabled` viejo |

---

## Checklist final

- [ ] RBM responde en su dominio (8080 intacto)
- [ ] `gym-postgres`, `gym-backend`, `gym-frontend` running
- [ ] `curl 127.0.0.1:8081/api/health` OK
- [ ] `https://gym...` carga la web
- [ ] `https://api.gym.../api/health` OK
- [ ] Login staff y/o afiliado probado
- [ ] `deploy/.env` con secretos fuertes
- [ ] Backup Postgres programado

Cuando todo esté marcado, **Gym está en producción**.
