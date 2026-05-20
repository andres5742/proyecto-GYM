FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG API_URL=https://api.gym.tudominio.com/api
ARG UPLOADS_BASE_URL=https://api.gym.tudominio.com
ARG ACCESS_DEVICE_KEY=gym-turnstile-dev-key
RUN test -n "${ACCESS_DEVICE_KEY}" || (echo "ERROR: ACCESS_DEVICE_KEY vacío en el build del frontend" && exit 1) && \
    sed -i \
    "s|http://localhost:8081/api|${API_URL}|g; \
     s|http://localhost:8081|${UPLOADS_BASE_URL}|g; \
     s|__ACCESS_DEVICE_KEY__|${ACCESS_DEVICE_KEY}|g" \
    src/environments/environment.production.ts && \
    ! grep -q '__ACCESS_DEVICE_KEY__' src/environments/environment.production.ts || \
    (echo "ERROR: no se sustituyó __ACCESS_DEVICE_KEY__ en environment.production.ts" && exit 1) && \
    npm run build -- --configuration production

FROM nginx:alpine
COPY deploy/nginx-frontend.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
