#!/bin/bash
set -e

# Cleanup function to stop containers on exit
cleanup() {
    echo
    echo "--- Stopping containers ---"
    docker stop webshop-demo db-demo > /dev/null 2>&1 || true
    docker rm webshop-demo db-demo > /dev/null 2>&1 || true
    docker network rm webshop-net > /dev/null 2>&1 || true
}
trap cleanup EXIT

echo "--- Building Webshop ---"
docker run --rm -v "$(pwd)/webshop:/usr/src/mymaven" -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests > /dev/null

echo "--- Starting Infrastructure ---"
docker network create webshop-net

# Start Postgres
docker run -d --rm --network webshop-net --name db-demo \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_USER=postgres \
    postgres:14-alpine

echo "Waiting for Database to be ready..."
sleep 5

# Start Webshop
echo "--- Starting Webshop ---"
docker run -d --rm --network webshop-net --name webshop-demo \
    -p 8000:8000 \
    -v "$(pwd)/webshop/target:/app" \
    -e DB_URL=jdbc:postgresql://db-demo:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    eclipse-temurin:21-jre java -jar /app/webshop-1.0-SNAPSHOT-jar-with-dependencies.jar

echo
echo "✅ Webshop is running at: http://localhost:8000"
echo "✅ Product list is at:    http://localhost:8000/products"
echo
echo "Press Ctrl+C to stop the server."

# Wait indefinitely so the script doesn't exit and trigger cleanup immediately
# We use 'tail -f /dev/null' to keep the script alive, but we want to see logs
docker logs -f webshop-demo
