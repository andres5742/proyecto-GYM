# Gym Manager

Sistema de gestión para gimnasio con **Spring Boot 3.5**, **Angular 21** y **PostgreSQL**.

## Estructura

```
proyecto gym/
├── backend/          # API REST (Java 17 + Spring Boot)
├── frontend/         # SPA Angular 21
└── docker-compose.yml
```

## Requisitos

- Java 17+
- Node.js 20+ y npm
- Docker y Docker Compose (para PostgreSQL)

## Base de datos (PostgreSQL)

```bash
docker compose up -d
```

Credenciales por defecto:

| Variable | Valor    |
|----------|----------|
| DB       | gym_db   |
| Usuario  | gym_user |
| Password | gym_pass |
| Puerto   | 5432     |

## Backend

```bash
cd backend
./mvnw spring-boot:run
```

API disponible en `http://localhost:8081`

### Endpoints principales

| Método | Ruta              | Descripción        |
|--------|-------------------|--------------------|
| GET    | /api/health       | Estado del servicio|
| GET    | /api/members      | Listar socios      |
| POST   | /api/members      | Crear socio        |
| PUT    | /api/members/{id} | Actualizar socio   |
| DELETE | /api/members/{id} | Eliminar socio     |
| GET    | /api/plans        | Listar planes      |
| POST   | /api/plans        | Crear plan         |
| GET    | /api/products     | Listar productos   |
| POST   | /api/products     | Crear producto     |
| PUT    | /api/products/{id}| Actualizar producto|
| PATCH  | /api/products/{id}/stock | Ajustar stock |
| DELETE | /api/products/{id}| Eliminar producto  |
| GET    | /api/employees    | Listar empleados   |
| POST   | /api/employees    | Crear empleado     |
| GET    | /api/sales        | Listar ventas      |
| GET    | /api/sales/summary| Resumen por medio de pago |
| POST   | /api/sales        | Registrar venta    |
| DELETE | /api/sales/{id}   | Anular venta (restaura stock) |

Al iniciar, se cargan planes de ejemplo y 3 empleados de prueba.

### Medios de pago en ventas

- Efectivo
- Nequi
- Bancolombia
- Pendiente de pago

Al registrar una venta se descuenta automáticamente el stock del inventario.

## Frontend

```bash
cd frontend
npm start
```

Aplicación en `http://localhost:4201`

## Variables de entorno (backend)

| Variable      | Default              |
|---------------|----------------------|
| DB_HOST       | localhost            |
| DB_PORT       | 5432                 |
| DB_NAME       | gym_db               |
| DB_USER       | gym_user             |
| DB_PASSWORD   | gym_pass             |
| SERVER_PORT   | 8081                 |
| CORS_ORIGINS  | http://localhost:4201|

## Próximos pasos sugeridos

- Autenticación (JWT / Spring Security)
- Control de pagos y facturación
- Registro de asistencia (check-in)
- Clases y horarios
- Panel de reportes
