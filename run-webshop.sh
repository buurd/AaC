#!/bin/bash
set -e

clear

# Cleanup function to stop containers on exit
cleanup() {
    echo
    echo "--- Stopping containers ---"
    docker stop webshop-demo pm-demo warehouse-demo reverse-proxy db-webshop db-pm db-warehouse > /dev/null 2>&1 || true
    docker rm webshop-demo pm-demo warehouse-demo reverse-proxy db-webshop db-pm db-warehouse > /dev/null 2>&1 || true
    docker network rm webshop-net > /dev/null 2>&1 || true
}
trap cleanup EXIT

echo "--- Setting up Certificates ---"
chmod +x infrastructure/setup-certs.sh
./infrastructure/setup-certs.sh

echo "--- Building Webshop ---"
docker run --rm -v "$(pwd)/webshop:/usr/src/mymaven" -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests > /dev/null

echo "--- Building Product Management System ---"
docker run --rm -v "$(pwd)/productManagementSystem:/usr/src/mymaven" -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests > /dev/null

echo "--- Building Warehouse Service ---"
docker run --rm -v "$(pwd)/warehouse:/usr/src/mymaven" -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests > /dev/null

echo "--- Starting Infrastructure ---"
docker network create webshop-net

# Start Webshop Postgres
docker run -d --rm --network webshop-net --name db-webshop \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_USER=postgres \
    postgres:14-alpine

# Start PM Postgres
docker run -d --rm --network webshop-net --name db-pm \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_USER=postgres \
    postgres:14-alpine

# Start Warehouse Postgres
docker run -d --rm --network webshop-net --name db-warehouse \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_USER=postgres \
    postgres:14-alpine

echo "Waiting for Databases to be ready (10s)..."
sleep 10

# Start Webshop
echo "--- Starting Webshop ---"
docker run -d --rm --network webshop-net --name webshop-demo \
    -v "$(pwd)/webshop/target:/app" \
    -e DB_URL=jdbc:postgresql://db-webshop:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    eclipse-temurin:21-jre java -jar /app/webshop-1.0-SNAPSHOT-jar-with-dependencies.jar

# Start Product Management System
echo "--- Starting Product Management System ---"
docker run -d --rm --network webshop-net --name pm-system \
    -v "$(pwd)/productManagementSystem/target:/app" \
    -e DB_URL=jdbc:postgresql://db-pm:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e WEBSHOP_API_URL=http://webshop-demo:8000/api/products/sync \
    -e WAREHOUSE_API_URL=http://warehouse-demo:8002/api/products/sync \
    eclipse-temurin:21-jre java -jar /app/productManagementSystem-1.0-SNAPSHOT-jar-with-dependencies.jar

# Start Warehouse Service
echo "--- Starting Warehouse Service ---"
docker run -d --rm --network webshop-net --name warehouse-demo \
    -v "$(pwd)/warehouse/target:/app" \
    -e DB_URL=jdbc:postgresql://db-warehouse:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e WEBSHOP_STOCK_API_URL=http://webshop-demo:8000/api/stock/sync \
    eclipse-temurin:21-jre java -jar /app/warehouse-1.0-SNAPSHOT-jar-with-dependencies.jar

# Start Reverse Proxy
echo "--- Starting Reverse Proxy (Nginx) ---"
docker run -d --rm --network webshop-net --name reverse-proxy \
    -p 8443:8443 -p 8444:8444 -p 8445:8445 \
    -v "$(pwd)/infrastructure/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" \
    -v "$(pwd)/infrastructure/nginx/certs:/etc/nginx/certs:ro" \
    nginx:alpine

echo
echo "✅ Webshop is running at:            https://localhost:8443"
echo "✅ Product Management is running at: https://localhost:8444"
echo "✅ Warehouse Service is running at:  https://localhost:8445"
echo
echo "Press Ctrl+C to stop the servers."

# Follow logs from all containers
docker logs -f webshop-demo &
docker logs -f pm-system &
docker logs -f warehouse-demo &
docker logs -f reverse-proxy &
wait
