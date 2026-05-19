# Seguridad — JWT y buenas prácticas

## Autenticación

- **JWT stateless** (sin sesión en servidor).
- Login: `POST /api/auth/login` → token en respuesta.
- Cliente: header `Authorization: Bearer <token>`.
- Perfil: `GET /api/auth/me` (requiere token válido).

## Variables de entorno (producción)

| Variable | Descripción |
|----------|-------------|
| `JWT_SECRET` | Clave HMAC ≥ 32 caracteres. **Obligatoria en producción.** |
| `JWT_EXPIRATION_MS` | Duración del token (default: 86400000 = 24 h) |
| `ACCESS_DEVICE_KEY` | Clave para torniquete (`X-Device-Key`) |

## Capas de autorización

1. `JwtAuthenticationFilter` — valida token; responde **401** si el Bearer es inválido.
2. `SecurityConfig` + `ApiAuthorizationRules` — roles por ruta.
3. `ModuleAccessFilter` — módulos habilitados por rol.
4. `@PreAuthorize` — reglas puntuales en controladores.

## Principios aplicados en auth

- **SRP**: `AuthService` (login), `JwtService` (tokens), `TokenStorageService` (FE), `AuthResponseFactory` (DTOs).
- **DRY**: etiquetas de rol en `UserRole.displayLabel()`; reglas HTTP en `ApiAuthorizationRules`.
- **KISS**: interceptor Angular solo reacciona a **401** con token presente.
- **YAGNI**: sin refresh token ni blacklist hasta que se necesiten.
