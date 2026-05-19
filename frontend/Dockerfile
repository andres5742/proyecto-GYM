FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG API_URL=https://api.gym.tudominio.com/api
ARG UPLOADS_BASE_URL=https://api.gym.tudominio.com
RUN sed -i "s|http://localhost:8081/api|${API_URL}|g; s|http://localhost:8081|${UPLOADS_BASE_URL}|g" \
    src/environments/environment.production.ts && \
    npm run build -- --configuration production

FROM nginx:alpine
COPY deploy/nginx-frontend.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
