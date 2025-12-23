#!/bin/bash
set -e

clear

# Cleanup function to stop containers on exit
cleanup() {
    echo
    echo "--- Stopping containers ---"
    docker stop webshop-demo pm-demo warehouse-demo order-service keycloak reverse-proxy loki promtail grafana db-webshop db-pm db-warehouse db-order > /dev/null 2>&1 || true
    docker rm webshop-demo pm-demo warehouse-demo order-service keycloak reverse-proxy loki promtail grafana db-webshop db-pm db-warehouse db-order > /dev/null 2>&1 || true
    # Don't remove the network if it was already there, but for this script we assume we own it.
    # However, to be safe, we try to remove it.
    docker network rm webshop-net > /dev/null 2>&1 || true
}
trap cleanup EXIT

echo "--- Setting up Certificates ---"
chmod +x infrastructure/setup-certs.sh
./infrastructure/setup-certs.sh

# Create a local Maven cache directory
mkdir -p ./m2-cache

echo "--- Building Webshop ---"
docker run --rm \
    -v "$(pwd)/webshop:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/root/.m2" \
    -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests

echo "--- Building Product Management System ---"
docker run --rm \
    -v "$(pwd)/productManagementSystem:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/root/.m2" \
    -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests

echo "--- Building Warehouse Service ---"
docker run --rm \
    -v "$(pwd)/warehouse:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/root/.m2" \
    -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests

echo "--- Building Order Service ---"
docker run --rm \
    -v "$(pwd)/orderService:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/root/.m2" \
    -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests

echo "--- Starting Infrastructure ---"
# Create network if it doesn't exist
docker network inspect webshop-net >/dev/null 2>&1 || docker network create webshop-net

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

# Start Order Postgres
docker run -d --rm --network webshop-net --name db-order \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_USER=postgres \
    postgres:14-alpine

# Start Observability Stack
echo "--- Starting Observability Stack ---"
# Run Loki as root to avoid permission issues with config mount
# Expose port 3100 for Promtail to access via host network
docker run -d --rm --network webshop-net --name loki -u 0 -p 3100:3100 \
    -v "$(pwd)/infrastructure/monitoring/loki-config.yaml:/etc/loki/local-config.yaml" \
    grafana/loki:2.9.4 -config.file=/etc/loki/local-config.yaml

echo "Waiting for Loki to start..."
sleep 5

# Add host.docker.internal mapping for Linux support
docker run -d --rm --network webshop-net --name promtail \
    --add-host host.docker.internal:host-gateway \
    -v "$(pwd)/infrastructure/monitoring/promtail-config.yaml:/etc/promtail/config.yml" \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /var/lib/docker/containers:/var/lib/docker/containers \
    grafana/promtail:2.9.4 -config.file=/etc/promtail/config.yml

docker run -d --rm --network webshop-net --name grafana \
    -p 3000:3000 \
    -e GF_SECURITY_ADMIN_PASSWORD=admin \
    -v "$(pwd)/infrastructure/monitoring/grafana-datasources.yaml:/etc/grafana/provisioning/datasources/datasources.yaml" \
    -v "$(pwd)/infrastructure/monitoring/grafana-dashboards.yaml:/etc/grafana/provisioning/dashboards/dashboards.yaml" \
    -v "$(pwd)/infrastructure/monitoring/dashboards:/var/lib/grafana/dashboards" \
    grafana/grafana:latest

echo "Waiting for Databases to be ready (10s)..."
sleep 10

# Start Keycloak
echo "--- Starting Keycloak ---"
docker run -d --rm --network webshop-net --name keycloak \
    -p 8084:8080 \
    -e KEYCLOAK_ADMIN=admin \
    -e KEYCLOAK_ADMIN_PASSWORD=admin \
    -e KC_PROXY=edge \
    -e KC_HOSTNAME_STRICT=false \
    -e KC_HOSTNAME_URL=https://localhost:8446 \
    -e KC_HTTP_ENABLED=true \
    -v "$(pwd)/infrastructure/keycloak/realm-export.json:/opt/keycloak/data/import/realm.json:ro" \
    quay.io/keycloak/keycloak:23.0.7 \
    start-dev --import-realm

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
docker run -d --rm --network webshop-net --name pm-demo \
    -v "$(pwd)/productManagementSystem/target:/app" \
    -e DB_URL=jdbc:postgresql://db-pm:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e WEBSHOP_API_URL=http://webshop-demo:8000/api/products/sync \
    -e WAREHOUSE_API_URL=http://warehouse-demo:8002/api/products/sync \
    -e CLIENT_ID=pm-client \
    -e CLIENT_SECRET=pm-secret \
    -e TOKEN_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token \
    eclipse-temurin:21-jre java -jar /app/productManagementSystem-1.0-SNAPSHOT-jar-with-dependencies.jar

# Start Warehouse Service
echo "--- Starting Warehouse Service ---"
docker run -d --rm --network webshop-net --name warehouse-demo \
    -v "$(pwd)/warehouse/target:/app" \
    -e DB_URL=jdbc:postgresql://db-warehouse:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e WEBSHOP_STOCK_API_URL=http://webshop-demo:8000/api/stock/sync \
    -e CLIENT_ID=warehouse-client \
    -e CLIENT_SECRET=warehouse-secret \
    -e TOKEN_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token \
    eclipse-temurin:21-jre java -jar /app/warehouse-1.0-SNAPSHOT-jar-with-dependencies.jar

# Start Order Service
echo "--- Starting Order Service ---"
docker run -d --rm --network webshop-net --name order-service \
    -v "$(pwd)/orderService/target:/app" \
    -e DB_URL=jdbc:postgresql://db-order:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e WAREHOUSE_RESERVE_URL=http://warehouse-demo:8002/api/stock/reserve \
    -e CLIENT_ID=order-client \
    -e CLIENT_SECRET=order-secret \
    -e TOKEN_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token \
    eclipse-temurin:21-jre java -jar /app/orderService-1.0-SNAPSHOT-jar-with-dependencies.jar

# Start Reverse Proxy
echo "--- Starting Reverse Proxy (Nginx) ---"
# Removed --rm to debug crash
docker run -d --network webshop-net --name reverse-proxy \
    -p 8443:8443 -p 8444:8444 -p 8445:8445 -p 8446:8446 -p 8447:8447 \
    -v "$(pwd)/infrastructure/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" \
    -v "$(pwd)/infrastructure/nginx/certs:/etc/nginx/certs:ro" \
    nginx:alpine

echo "Waiting for Keycloak to be ready (20s)..."
sleep 20

# Check if proxy is running
if ! docker ps | grep -q reverse-proxy; then
    echo "🔴 Reverse Proxy failed to start!"
    docker logs reverse-proxy
    exit 1
fi

echo
echo "✅ Webshop is running at:            https://localhost:8443"
echo "✅ Product Management is running at: https://localhost:8444"
echo "   (Login: p-user / p-user)"
echo "✅ Warehouse Service is running at:  https://localhost:8445"
echo "   (Login: w-user / w-user)"
echo "✅ Order Service is running at:      https://localhost:8447"
echo "   (Login: o-user / o-user)"
echo "✅ Keycloak is running at:           https://localhost:8446"
echo "   (Admin Console: https://localhost:8446/admin/)"
echo "✅ Grafana is running at:            http://localhost:3000"
echo "   (Login: admin / admin)"
echo

# Debug: Check Loki labels
echo "--- Checking Loki Labels ---"
docker run --rm --network webshop-net curlimages/curl:7.78.0 curl -s http://loki:3100/loki/api/v1/labels
echo
echo

echo "Press Ctrl+C to stop the servers."

# Follow logs from all containers
docker logs -f webshop-demo &
docker logs -f pm-demo &
docker logs -f warehouse-demo &
docker logs -f order-service &
docker logs -f keycloak &
docker logs -f reverse-proxy &
docker logs -f promtail &
docker logs -f loki &
wait
