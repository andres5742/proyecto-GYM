# Desplegar Gym (y convivir con RBM) en tu VPS

Tu servidor **ya tiene producción** en el puerto **8080** (`/opt/spring-angular-app`).  
**No lo detengas ni cambies su puerto.**

## Mapa de puertos (sin conflictos)

| Aplicación | Qué es hoy | Puerto interno | Acceso público |
|------------|------------|----------------|----------------|
| **RBM / app actual** | Java manual | `8080` | Tu dominio actual → Nginx → `8080` |
| **Gym – backend** | Docker | `127.0.0.1:8081` | `api.gym.tudominio.com` → Nginx → `8081` |
| **Gym – frontend** | Docker | `127.0.0.1:3001` | `gym.tudominio.com` → Nginx → `3001` |
| **Gym – PostgreSQL** | Docker | solo red Docker | **no** publicar al internet |
| **RBM futuro en Docker** (opcional) | | `8082` / `3002` | otro subdominio |

> **No instales Nginx Proxy Manager en 80/443** si ya tienes Nginx del sistema: chocarían. Usa el Nginx que ya tienes y solo agrega sitios nuevos.

---

## Paso 1 — Estructura en el servidor

```bash
sudo mkdir -p /apps/gym-app
sudo mkdir -p /apps/rbm-app   # cuando migres RBM a Docker
sudo chown -R $USER:$USER /apps
```

Clona o sube el código del gym:

```bash
cd /apps/gym-app
git clone https://github.com/andres5742/proyecto-GYM.git .
# o: rsync/scp desde tu PC
```

---

## Paso 2 — Variables de entorno (gym)

```bash
cd /apps/gym-app
cp deploy/.env.example deploy/.env
nano deploy/.env
```

Ajusta al menos:

- `JWT_SECRET` — clave larga y aleatoria (≥ 32 caracteres)
- `CORS_ORIGINS` — `https://gym.tudominio.com`
- `POSTGRES_PASSWORD` — contraseña fuerte
- Dominios reales en el build del frontend (ver `deploy/.env.example`)

---

## Paso 3 — Levantar Gym con Docker (sin tocar 8080)

```bash
cd /apps/gym-app
docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d --build
docker compose -f docker-compose.prod.yml ps
docker logs gym-backend --tail 50
```

Comprueba en el servidor:

```bash
curl -s http://127.0.0.1:8081/api/health
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:3001/
```

---

## Paso 4 — Nginx (agregar sitios, no reemplazar)

Crea `/etc/nginx/sites-available/gym`:

```nginx
# Frontend
server {
    listen 443 ssl http2;
    server_name gym.tudominio.com;

    # ssl_certificate ... (certbot o tus rutas actuales)

    location / {
        proxy_pass http://127.0.0.1:3001;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# API
server {
    listen 443 ssl http2;
    server_name api.gym.tudominio.com;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 10M;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/gym /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

En **Hostinger DNS**: registros `A` de `gym` y `api.gym` → IP del VPS.

---

## Paso 5 — App actual (RBM en 8080)

**No cambies nada** mientras funcione:

```bash
ps aux | grep java
sudo ss -tulpn | grep 8080
```

Sigue siendo:

```
tudominio-rbm.com → Nginx → localhost:8080
```

---

## Paso 6 — Segunda app (RBM en Docker) más adelante

Cuando quieras dockerizar RBM **sin tumbar la actual**:

1. Levanta la nueva versión en **8082** (backend) y **3002** (frontend).
2. Prueba con un subdominio de prueba (`rbm-v2.tudominio.com`).
3. Cuando esté bien, cambia el `proxy_pass` del dominio principal de `8080` → `8082`.
4. Apaga el proceso Java viejo **solo entonces**.

---

## Comandos útiles

```bash
# Ver solo contenedores gym
docker ps --filter "name=gym"

# Reiniciar gym tras git pull
cd /apps/gym-app && git pull
docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d --build

# Logs
docker logs -f gym-backend
docker logs -f gym-frontend

# Backup Postgres
docker exec gym-postgres pg_dump -U gym_user gym_db > /backups/gym_$(date +%F).sql
```

---

## Swap (recomendado para Java)

```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

---

## Resumen

1. **8080** = app actual intacta.  
2. **Gym** = Docker en **8081** + **3001**, Postgres solo interno.  
3. **Nginx del sistema** = nuevos `server_name`, no otro Nginx en 80/443.  
4. **RBM en Docker** = después, en **8082**, con corte controlado.
