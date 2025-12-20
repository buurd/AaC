#!/bin/bash
set -e

clear

# Cleanup function to stop containers on exit
cleanup() {
    echo
    echo "--- Stopping containers ---"
    docker stop webshop-demo pm-demo warehouse-demo db-webshop db-pm db-warehouse > /dev/null 2>&1 || true
    docker rm webshop-demo pm-demo warehouse-demo db-webshop db-pm db-warehouse > /dev/null 2>&1 || true
    docker network rm webshop-net > /dev/null 2>&1 || true
}
trap cleanup EXIT

echo "--- Building Webshop ---"
docker run --rm -v "$(pwd)/webshop:/usr/src/mymaven" -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests

echo "--- Building Product Management System ---"
docker run --rm -v "$(pwd)/productManagementSystem:/usr/src/mymaven" -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests

echo "--- Building Warehouse Service ---"
docker run --rm -v "$(pwd)/warehouse:/usr/src/mymaven" -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests

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
    -p 8081:8000 \
    -v "$(pwd)/webshop/target:/app" \
    -e DB_URL=jdbc:postgresql://db-webshop:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    eclipse-temurin:21-jre java -jar /app/webshop-1.0-SNAPSHOT-jar-with-dependencies.jar

# Start Product Management System
echo "--- Starting Product Management System ---"
docker run -d --rm --network webshop-net --name pm-demo \
    -p 8082:8001 \
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
    -p 8083:8002 \
    -v "$(pwd)/warehouse/target:/app" \
    -e DB_URL=jdbc:postgresql://db-warehouse:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e WEBSHOP_STOCK_API_URL=http://webshop-demo:8000/api/stock/sync \
    eclipse-temurin:21-jre java -jar /app/warehouse-1.0-SNAPSHOT-jar-with-dependencies.jar

echo
echo "✅ Webshop is running at:            http://localhost:8081"
echo "✅ Webshop Product list:             http://localhost:8081/products"
echo "✅ Product Management is running at: http://localhost:8082"
echo "✅ PM Product list:                  http://localhost:8082/products"
echo "✅ Warehouse Service is running at:  http://localhost:8083"
echo
echo "Press Ctrl+C to stop the servers."

# Follow logs from all containers
docker logs -f webshop-demo &
docker logs -f pm-demo &
docker logs -f warehouse-demo &
wait
