<!-- En tu PC (después de programar) -->

cd "/home/david/proyectos pagos/proyecto gym"
git add .
git commit -m "Descripción del cambio"
git push origin main


<!-- En el VPS (SSH) -->

cd /apps/gym-app
git pull
docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d --build


<!-- Comprobar -->

docker compose -f docker-compose.prod.yml ps
docker logs gym-backend --tail 30
curl -s https://sportgymr10.com/api/health


http://72.61.65.92:9000/#!/auth