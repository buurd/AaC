#!/bin/bash
set -e
clear

START_TIME=$(date +%s)

# Redirect all output to log file and stdout (overwrite)
exec > >(tee logs/validate-architecture.log) 2>&1

# --- Pre-Cleanup ---
echo "--- Checking for leftover containers ---"
CONTAINERS="webshop-demo pm-demo warehouse-demo order-service loyalty-service keycloak reverse-proxy db_webshop db_pm db_warehouse db_order db_loyalty db-webshop db-pm db-warehouse db-order db-loyalty loki promtail grafana"

for container in $CONTAINERS; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "Removing leftover container: $container"
        docker stop $container > /dev/null 2>&1 || true
        docker rm $container > /dev/null 2>&1 || true
    fi
done

# --- Cleanup ---
cleanup() {
    echo
    echo "--- Cleaning up temporary files ---"
    rm -f workspace.json project-files.json requirements.json implementation-input.json code-structure-input.json code-structure-files.json relation-input.json test-content-input.json test-files.json k8s-manifests.json k8s-validation-input.json coverage-input.json coverage-data.json
    rm -f structurizr/workspace.json

    echo "--- Stopping containers ---"
    if [ -n "$WEBSHOP_CONTAINER_ID" ]; then docker stop "$WEBSHOP_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$PM_CONTAINER_ID" ]; then docker stop "$PM_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$WAREHOUSE_CONTAINER_ID" ]; then docker stop "$WAREHOUSE_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$ORDER_CONTAINER_ID" ]; then docker stop "$ORDER_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$LOYALTY_CONTAINER_ID" ]; then docker stop "$LOYALTY_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$KEYCLOAK_CONTAINER_ID" ]; then docker stop "$KEYCLOAK_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$PROXY_CONTAINER_ID" ]; then docker stop "$PROXY_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$DB_WEBSHOP_ID" ]; then docker stop "$DB_WEBSHOP_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$DB_PM_ID" ]; then docker stop "$DB_PM_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$DB_WAREHOUSE_ID" ]; then docker stop "$DB_WAREHOUSE_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$DB_ORDER_ID" ]; then docker stop "$DB_ORDER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$DB_LOYALTY_ID" ]; then docker stop "$DB_LOYALTY_ID" > /dev/null 2>&1 || true; fi

    docker stop webshop-demo pm-demo warehouse-demo order-service loyalty-service keycloak reverse-proxy db_webshop db_pm db_warehouse db_order db_loyalty > /dev/null 2>&1 || true

    if [ -n "$NETWORK_NAME" ]; then
        echo "Removing network: $NETWORK_NAME"
        docker network rm "$NETWORK_NAME" > /dev/null 2>&1 || true
    fi

    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    echo "Total execution time: ${DURATION} seconds"
}
trap cleanup EXIT

# --- Setup ---
run_opa_validation() {
    local title="$1"
    local input_file="$2"
    local policy_file="$3"
    local query="$4"

    echo
    echo "--- Running: $title ---"

    set +e
    RESULT=$(docker run --rm -v "$(pwd):/project" openpolicyagent/opa eval \
        -i "$input_file" \
        -d "$policy_file" \
        "$query" 2>&1)
    OPA_EXIT_CODE=$?
    set -e

    if [ $OPA_EXIT_CODE -ne 0 ]; then
        echo "🔴 OPA Execution Failed (Exit Code: $OPA_EXIT_CODE):"
        echo "$RESULT"
        exit 1
    fi

    if echo "$RESULT" | grep -q "\"error\":"; then
         echo "🔴 OPA Returned Errors:"
         echo "$RESULT"
         exit 1
    fi

    VIOLATIONS=$(echo "$RESULT" | jq '.result[0].expressions[0].value')

    if [ -z "$VIOLATIONS" ] || [ "$VIOLATIONS" == "null" ]; then
        echo "⚠️  Unexpected OPA output format:"
        echo "$RESULT"
        exit 1
    fi

    if [ "$(echo "$VIOLATIONS" | jq 'length')" -gt 0 ]; then
        echo "🔴 Validation Failed:"
        echo "$VIOLATIONS" | jq .
        exit 1
    else
        echo "✅ Validation Passed."
    fi
}

run_runtime_check() {
    local title="$1"
    local url="$2"
    local expected_status="$3"
    local response_contains="$4"
    local container_id="$5"

    echo
    echo "--- Running: $title ---"
    echo "Checking URL: $url"

    if [ -n "$container_id" ]; then
        if ! docker ps -q --no-trunc | grep -q "$container_id"; then
             echo "🔴 Validation Failed: Container $container_id is not running."
             echo "--- Container Logs ---"
             docker logs "$container_id"
             exit 1
        fi
    fi

    set +e
    RESPONSE=$(docker run --rm --network="host" curlimages/curl:7.78.0 curl -k -v -s -w "\\n%{http_code}" "$url" 2>&1)
    CURL_EXIT_CODE=$?
    set -e

    if [ $CURL_EXIT_CODE -ne 0 ]; then
        echo "🔴 Validation Failed: Curl command failed (Exit Code: $CURL_EXIT_CODE)"
        echo "Output: $RESPONSE"
        if [ -n "$container_id" ]; then
            echo "--- Container Logs ($container_id) ---"
            docker logs "$container_id" | tail -n 20
        fi
        exit 1
    fi

    HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
    HTTP_BODY=$(echo "$RESPONSE" | sed '$d')

    echo "Server returned status: $HTTP_STATUS"

    if [ "$HTTP_STATUS" -ne "$expected_status" ]; then
        echo "🔴 Validation Failed: Expected status $expected_status, got $HTTP_STATUS"
        echo "Response Body: $HTTP_BODY"
        if [ -n "$container_id" ]; then
            echo "--- Container Logs ($container_id) ---"
            docker logs "$container_id" | tail -n 20
        fi
        exit 1
    fi

    if [ -n "$response_contains" ]; then
        if echo "$HTTP_BODY" | grep -q "$response_contains"; then
            echo "Response body contains expected text: '$response_contains'"
        else
            echo "🔴 Validation Failed: Response body did not contain expected text."
            echo "Expected: '$response_contains'"
            echo "Got: '$HTTP_BODY'"
            exit 1
        fi
    fi

    echo "✅ Validation Passed."
}


# --- Pre-computation Steps ---
echo "Step 1: Exporting Structurizr DSL to JSON..."
docker run --rm -u "$(id -u):$(id -g)" -v "$(pwd)/structurizr:/usr/local/structurizr" structurizr/cli export -workspace workspace.dsl -format json > /dev/null
mv -f structurizr/workspace.json workspace.json

echo "Step 2: Generating file system view..."
find . -type d \( -path ./.git -o -path ./production-data -o -path ./m2-cache -o -path ./logs -o -name target \) -prune -o -type f -print | sed 's|^\./||' | jq -R . | jq -s . > project-files.json

echo "Step 3: Aggregating requirements..."
docker run --rm -u "$(id -u):$(id -g)" -v "$(pwd):/workdir" mikefarah/yq eval-all -o=json '. as $doc ireduce ({}; .requirements += [$doc])' requirements/*.yaml > requirements.json

echo "Step 4: Combining inputs for validations..."
jq -n '{files: $files, reqs: $reqs}' --slurpfile files project-files.json --slurpfile reqs requirements.json > implementation-input.json
find . -name "*.java" -print0 | xargs -0 -I {} jq -Rs --arg path "{}" '{$path: .}' {} | jq -s 'add' > code-structure-files.json
jq -n '{files: $files, reqs: $reqs}' --slurpfile files code-structure-files.json --slurpfile reqs requirements.json > code-structure-input.json
find e2e-tests -name "*.js" -print0 | xargs -0 -I {} jq -Rs --arg path "{}" '{$path: .}' {} | jq -s 'add' > test-files.json
jq -n '{files: $files, reqs: $reqs}' --slurpfile files test-files.json --slurpfile reqs requirements.json > test-content-input.json
jq -n '{model: $model, reqs: $reqs}' --slurpfile model workspace.json --slurpfile reqs requirements.json > relation-input.json

echo "Step 5: Parsing Kubernetes manifests..."
docker run --rm -u "$(id -u):$(id -g)" -v "$(pwd):/workdir" mikefarah/yq eval-all -o=json '.' infrastructure/k8s/*.yaml | jq -s . > k8s-manifests.json

echo "Step 6: Combining inputs for K8s validation..."
jq -n '{k8s: $k8s[0], reqs: $reqs}' --slurpfile k8s k8s-manifests.json --slurpfile reqs requirements.json > k8s-validation-input.json


# --- Static Validations ---
run_opa_validation "Traceability Validation" "/project/requirements.json" "/project/policies/check_traceability.rego" "data.requirements.traceability.violation"
run_opa_validation "Relation Validation" "/project/relation-input.json" "/project/policies/check_relation.rego" "data.structurizr.relation.violation"
run_opa_validation "Container Validation" "/project/relation-input.json" "/project/policies/check_containers.rego" "data.structurizr.containers.violation"
run_opa_validation "Component Validation" "/project/workspace.json" "/project/policies/check_components.rego" "data.structurizr.components.violation"
run_opa_validation "Implementation Validation" "/project/implementation-input.json" "/project/policies/check_implementation.rego" "data.project_files.violation"
run_opa_validation "Code Structure Validation" "/project/code-structure-input.json" "/project/policies/check_class_structure.rego" "data.code.structure.violation"
run_opa_validation "Dependency Validation" "/project/code-structure-input.json" "/project/policies/check_dependency.rego" "data.code.dependency.violation"
run_opa_validation "HTTPS Usage Validation" "/project/test-content-input.json" "/project/policies/check_https_usage.rego" "data.security.https.violation"
run_opa_validation "Pact Verification Validation" "/project/implementation-input.json" "/project/policies/check_pact_verification.rego" "data.integration.pact.violation"
run_opa_validation "Kubernetes Deployment Validation" "/project/k8s-validation-input.json" "/project/policies/check_k8s_deployment.rego" "data.k8s.deployment.violation"

# --- Vulnerability Scanning ---
echo
echo "--- Running: Vulnerability Scanning ---"
chmod +x scan-vulnerabilities.sh
./scan-vulnerabilities.sh

# --- Unit Validation ---
echo
echo "--- Running: Unit Validation ---"
mkdir -p m2-cache

# Fix permissions for m2-cache (in case it was created by root in run-webshop.sh)
docker run --rm -v "$(pwd):/workdir" busybox chown -R $(id -u):$(id -g) /workdir/m2-cache

MODULES="webshop productManagementSystem warehouse orderService loyaltyService"

# Clean up target directories to avoid permission issues
for module in $MODULES; do
    if [ -d "applications/$module" ]; then
         docker run --rm -v "$(pwd)/applications/$module:/usr/src/mymaven" maven:3.9.6-eclipse-temurin-21 rm -rf /usr/src/mymaven/target
    fi
done

for module in $MODULES; do
    if [ -d "applications/$module" ] && [ -f "applications/$module/pom.xml" ]; then
        echo "  Running unit tests for $module..."
        if ! docker run --rm \
            -u "$(id -u):$(id -g)" \
            -e HOME=/tmp \
            -e MAVEN_CONFIG=/tmp/.m2 \
            -v "$(pwd)/applications/$module:/usr/src/mymaven" \
            -v "$(pwd)/m2-cache:/tmp/.m2" \
            -v /etc/passwd:/etc/passwd:ro \
            -v /etc/group:/etc/group:ro \
            -w /usr/src/mymaven \
            maven:3.9.6-eclipse-temurin-21 mvn clean test -Dgroups='!pact-consumer & !pact-provider' -Dmaven.repo.local=/tmp/.m2/repository; then
            echo "🔴 Unit Tests Failed in $module"
            exit 1
        fi
    fi
done

echo "✅ Unit Validation Passed."

# --- Contract Validation (Pact) ---
echo
echo "--- Running: Contract Validation (Pact) ---"
mkdir -p pacts
MODULES="webshop productManagementSystem warehouse orderService loyaltyService"

echo "Phase 1: Generating Contracts (Consumer Tests)..."
for module in $MODULES; do
    if [ -d "applications/$module" ] && [ -f "applications/$module/pom.xml" ]; then
        echo "  Scanning $module for consumer tests..."
        # Removed clean to preserve coverage data
        if ! docker run --rm \
            -u "$(id -u):$(id -g)" \
            -e HOME=/tmp \
            -e MAVEN_CONFIG=/tmp/.m2 \
            -v "$(pwd)/applications/$module:/usr/src/mymaven" \
            -v "$(pwd)/pacts:/usr/src/pacts" \
            -v "$(pwd)/m2-cache:/tmp/.m2" \
            -v /etc/passwd:/etc/passwd:ro \
            -v /etc/group:/etc/group:ro \
            -w /usr/src/mymaven \
            maven:3.9.6-eclipse-temurin-21 mvn test -Dgroups=pact-consumer -DfailIfNoTests=false -Dmaven.repo.local=/tmp/.m2/repository; then
            echo "🔴 Consumer Contract Tests Failed in $module"
            exit 1
        fi
    fi
done

echo "Phase 2: Verifying Contracts (Provider Tests)..."
for module in $MODULES; do
    if [ -d "applications/$module" ] && [ -f "applications/$module/pom.xml" ]; then
        echo "  Scanning $module for provider tests..."
        # Removed clean to preserve coverage data
        if ! docker run --rm \
            -u "$(id -u):$(id -g)" \
            -e HOME=/tmp \
            -e MAVEN_CONFIG=/tmp/.m2 \
            -v "$(pwd)/applications/$module:/usr/src/mymaven" \
            -v "$(pwd)/pacts:/usr/src/pacts" \
            -v "$(pwd)/m2-cache:/tmp/.m2" \
            -v /etc/passwd:/etc/passwd:ro \
            -v /etc/group:/etc/group:ro \
            -w /usr/src/mymaven \
            maven:3.9.6-eclipse-temurin-21 mvn test -Dgroups=pact-provider -DfailIfNoTests=false -Dmaven.repo.local=/tmp/.m2/repository; then
            echo "🔴 Provider Contract Verification Failed in $module"
            exit 1
        fi
    fi
done

echo "✅ Contract Validation Passed."

# --- Code Coverage Validation ---
echo
echo "--- Running: Code Coverage Validation ---"
# Generate coverage report (using Jacoco)
for module in $MODULES; do
    if [ -d "applications/$module" ] && [ -f "applications/$module/pom.xml" ]; then
        echo "  Generating coverage for $module..."
        # We run 'jacoco:report' to ensure the report is generated from the accumulated exec file.
        if ! docker run --rm \
            -u "$(id -u):$(id -g)" \
            -e HOME=/tmp \
            -e MAVEN_CONFIG=/tmp/.m2 \
            -v "$(pwd)/applications/$module:/usr/src/mymaven" \
            -v "$(pwd)/m2-cache:/tmp/.m2" \
            -v /etc/passwd:/etc/passwd:ro \
            -v /etc/group:/etc/group:ro \
            -w /usr/src/mymaven \
            maven:3.9.6-eclipse-temurin-21 mvn jacoco:report -Dmaven.repo.local=/tmp/.m2/repository > /dev/null; then
             echo "🔴 Failed to generate coverage report for $module"
             exit 1
        fi
    fi
done

# Extract coverage data and prepare input for OPA
echo "  Extracting coverage data..."
# We'll use a simple python script or similar to parse the XML/CSV report and create a JSON
# For simplicity, let's assume we parse the index.html or csv. CSV is easier.
# Jacoco CSV report is usually at target/site/jacoco/jacoco.csv

echo "{" > coverage-data.json
echo "  \"coverage_data\": {" >> coverage-data.json
FIRST=1
for module in $MODULES; do
    CSV_FILE="applications/$module/target/site/jacoco/jacoco.csv"
    if [ -f "$CSV_FILE" ]; then
        # Calculate total instruction coverage
        # CSV format: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,...
        # We sum up missed and covered instructions
        STATS=$(awk -F, '{missed+=$4; covered+=$5} END {print missed, covered}' "$CSV_FILE")
        MISSED=$(echo $STATS | cut -d' ' -f1)
        COVERED=$(echo $STATS | cut -d' ' -f2)
        TOTAL=$((MISSED + COVERED))

        if [ "$TOTAL" -gt 0 ]; then
            # Round to nearest integer to match HTML report behavior
            PERCENTAGE=$(awk "BEGIN {printf \"%.0f\", 100 * $COVERED / $TOTAL}")
        else
            PERCENTAGE=0
        fi

        if [ "$FIRST" -eq 0 ]; then echo "," >> coverage-data.json; fi
        echo "    \"$module\": { \"percentage\": $PERCENTAGE }" >> coverage-data.json
        FIRST=0
    else
        echo "⚠️  No coverage report found for $module"
    fi
done
echo "  }," >> coverage-data.json
echo "  \"applications\": [\"webshop\", \"productManagementSystem\", \"warehouse\", \"orderService\", \"loyaltyService\"]" >> coverage-data.json
echo "}" >> coverage-data.json

mv coverage-data.json coverage-input.json

# Run OPA check
run_opa_validation "Code Coverage Validation" "/project/coverage-input.json" "/project/policies/check_code_coverage.rego" "data.check_code_coverage.deny"


# --- Runtime Validation ---
echo
echo "--- Preparing for Runtime Validations ---"
echo "Setting up Certificates..."
chmod +x infrastructure/setup-certs.sh
./infrastructure/setup-certs.sh

mkdir -p ./m2-cache

echo "Compiling Webshop application..."
docker run --rm \
    -u "$(id -u):$(id -g)" \
    -e HOME=/tmp \
    -e MAVEN_CONFIG=/tmp/.m2 \
    -v "$(pwd)/applications/webshop:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/tmp/.m2" \
    -v /etc/passwd:/etc/passwd:ro \
    -v /etc/group:/etc/group:ro \
    -w /usr/src/mymaven \
    maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository

echo "Compiling Product Management application..."
docker run --rm \
    -u "$(id -u):$(id -g)" \
    -e HOME=/tmp \
    -e MAVEN_CONFIG=/tmp/.m2 \
    -v "$(pwd)/applications/productManagementSystem:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/tmp/.m2" \
    -v /etc/passwd:/etc/passwd:ro \
    -v /etc/group:/etc/group:ro \
    -w /usr/src/mymaven \
    maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository

echo "Compiling Warehouse Service..."
docker run --rm \
    -u "$(id -u):$(id -g)" \
    -e HOME=/tmp \
    -e MAVEN_CONFIG=/tmp/.m2 \
    -v "$(pwd)/applications/warehouse:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/tmp/.m2" \
    -v /etc/passwd:/etc/passwd:ro \
    -v /etc/group:/etc/group:ro \
    -w /usr/src/mymaven \
    maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository

echo "Compiling Order Service..."
docker run --rm \
    -u "$(id -u):$(id -g)" \
    -e HOME=/tmp \
    -e MAVEN_CONFIG=/tmp/.m2 \
    -v "$(pwd)/applications/orderService:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/tmp/.m2" \
    -v /etc/passwd:/etc/passwd:ro \
    -v /etc/group:/etc/group:ro \
    -w /usr/src/mymaven \
    maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository

echo "Compiling Loyalty Service..."
docker run --rm \
    -u "$(id -u):$(id -g)" \
    -e HOME=/tmp \
    -e MAVEN_CONFIG=/tmp/.m2 \
    -v "$(pwd)/applications/loyaltyService:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/tmp/.m2" \
    -v /etc/passwd:/etc/passwd:ro \
    -v /etc/group:/etc/group:ro \
    -w /usr/src/mymaven \
    maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2/repository

NETWORK_NAME="webshop-net-$$"
docker network create $NETWORK_NAME

echo "Starting Webshop Database container..."
DB_WEBSHOP_ID=$(docker run -d --network $NETWORK_NAME --name db_webshop -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres postgres:14-alpine)

echo "Starting PM Database container..."
DB_PM_ID=$(docker run -d --network $NETWORK_NAME --name db_pm -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres postgres:14-alpine)

echo "Starting Warehouse Database container..."
DB_WAREHOUSE_ID=$(docker run -d --network $NETWORK_NAME --name db_warehouse -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres postgres:14-alpine)

echo "Starting Order Database container..."
DB_ORDER_ID=$(docker run -d --network $NETWORK_NAME --name db_order -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres postgres:14-alpine)

echo "Starting Loyalty Database container..."
DB_LOYALTY_ID=$(docker run -d --network $NETWORK_NAME --name db_loyalty -e POSTGRES_DB=loyalty_db -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres postgres:14-alpine)

echo "Waiting for Databases to initialize (15s)..."
sleep 15

echo "Starting Keycloak container..."
KEYCLOAK_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name keycloak \
    -e KEYCLOAK_ADMIN=admin \
    -e KEYCLOAK_ADMIN_PASSWORD=admin \
    -e KC_PROXY=edge \
    -e KC_HOSTNAME_STRICT=false \
    -e KC_HOSTNAME_STRICT_BACKCHANNEL=true \
    -e KC_HOSTNAME_URL=https://reverse-proxy:8446 \
    -e KC_HTTP_ENABLED=true \
    -v "$(pwd)/infrastructure/keycloak/realm-export.json:/opt/keycloak/data/import/realm.json:ro" \
    quay.io/keycloak/keycloak:23.0.7 \
    start-dev --import-realm)

echo "Starting Webshop server..."
WEBSHOP_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name webshop-demo \
    -v "$(pwd)/applications/webshop/target:/app" \
    -e DB_URL=jdbc:postgresql://db_webshop:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e JWKS_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/certs \
    -e ISSUER_URL=https://reverse-proxy:8446/realms/webshop-realm \
    eclipse-temurin:21-jre java -jar /app/webshop-1.0-SNAPSHOT-jar-with-dependencies.jar)

echo "Starting Product Management server..."
PM_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name pm-demo \
    -v "$(pwd)/applications/productManagementSystem/target:/app" \
    -e DB_URL=jdbc:postgresql://db_pm:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e WEBSHOP_API_URL=http://webshop-demo:8000/api/products/sync \
    -e WAREHOUSE_API_URL=http://warehouse-demo:8002/api/products/sync \
    -e JWKS_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/certs \
    -e ISSUER_URL=https://reverse-proxy:8446/realms/webshop-realm \
    -e CLIENT_ID=pm-client \
    -e CLIENT_SECRET=pm-secret \
    -e TOKEN_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token \
    eclipse-temurin:21-jre java -jar /app/productManagementSystem-1.0-SNAPSHOT-jar-with-dependencies.jar)

echo "Starting Warehouse Service..."
WAREHOUSE_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name warehouse-demo \
    -v "$(pwd)/applications/warehouse/target:/app" \
    -e DB_URL=jdbc:postgresql://db_warehouse:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e WEBSHOP_STOCK_API_URL=http://webshop-demo:8000/api/stock/sync \
    -e JWKS_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/certs \
    -e ISSUER_URL=https://reverse-proxy:8446/realms/webshop-realm \
    -e CLIENT_ID=warehouse-client \
    -e CLIENT_SECRET=warehouse-secret \
    -e TOKEN_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token \
    eclipse-temurin:21-jre java -jar /app/warehouse-1.0-SNAPSHOT-jar-with-dependencies.jar)

echo "Starting Order Service..."
ORDER_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name order-service \
    -v "$(pwd)/applications/orderService/target:/app" \
    -e DB_URL=jdbc:postgresql://db_order:5432/postgres \
    -e DB_USER=postgres \
    -e WAREHOUSE_RESERVE_URL=http://warehouse-demo:8002/api/stock/reserve \
    -e JWKS_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/certs \
    -e ISSUER_URL=https://reverse-proxy:8446/realms/webshop-realm \
    -e CLIENT_ID=order-client \
    -e CLIENT_SECRET=order-secret \
    -e TOKEN_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token \
    eclipse-temurin:21-jre java -jar /app/orderService-1.0-SNAPSHOT-jar-with-dependencies.jar)

echo "Starting Loyalty Service..."
LOYALTY_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name loyalty-service \
    -v "$(pwd)/applications/loyaltyService/target:/app" \
    -e DB_URL=jdbc:postgresql://db_loyalty:5432/loyalty_db \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    eclipse-temurin:21-jre java -jar /app/loyalty-service-1.0-SNAPSHOT-jar-with-dependencies.jar)

echo "Waiting 2 seconds for Docker DNS to propagate..."
sleep 2

echo "Starting Reverse Proxy (Nginx)..."
PROXY_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name reverse-proxy \
    -p 8443:8443 -p 8444:8444 -p 8445:8445 -p 8446:8446 -p 8447:8447 -p 8448:8448 \
    -v "$(pwd)/infrastructure/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" \
    -v "$(pwd)/infrastructure/nginx/certs:/etc/nginx/certs:ro" \
    nginx:alpine)

echo "Waiting for services to initialize (60s)..."
sleep 60

if ! docker ps -q --no-trunc | grep -q "$PROXY_CONTAINER_ID"; then
    echo "🔴 Reverse Proxy failed to start!"
    docker logs "$PROXY_CONTAINER_ID"
    exit 1
fi

run_runtime_check "Runtime Validation (REQ-005) - HTTPS" "https://localhost:8443/" "200" "" "$WEBSHOP_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-007) - HTTPS" "https://localhost:8443/products" "200" "Classic T-Shirt" "$WEBSHOP_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-014) - HTTPS (Secured)" "https://localhost:8444/products" "302" "" "$PM_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-031) - HTTPS (Secured)" "https://localhost:8445/products" "302" "" "$WAREHOUSE_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-034) - HTTPS (Secured)" "https://localhost:8445/products" "302" "" "$WAREHOUSE_CONTAINER_ID"
run_runtime_check "Runtime Validation (Keycloak) - HTTPS" "https://localhost:8446/" "200" "Welcome to Keycloak" "$KEYCLOAK_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-051) - HTTPS (Secured)" "https://localhost:8447/" "200" "Order Service" "$ORDER_CONTAINER_ID"
run_runtime_check "Runtime Validation (Loyalty) - HTTPS" "https://localhost:8448/" "200" "Loyalty Admin Dashboard" "$LOYALTY_CONTAINER_ID"

# --- Functional Validation (Cypress) ---
echo
echo "--- Running: Functional Validation (Cypress) ---"
echo "Running Cypress tests against https://reverse-proxy:8444..."

set +e
docker run --rm --network $NETWORK_NAME \
    -v "$(pwd)/e2e-tests:/e2e" \
    -w /e2e \
    cypress/included:13.6.6 \
    --browser chrome
CYPRESS_EXIT_CODE=$?
set -e

if [ $CYPRESS_EXIT_CODE -ne 0 ]; then
    echo "🔴 Functional Validation Failed: Cypress tests failed."
    echo "--- Webshop Logs ---"
    docker logs "$WEBSHOP_CONTAINER_ID" | tail -n 200
    echo "--- PM System Logs ---"
    docker logs "$PM_CONTAINER_ID" | tail -n 200
    echo "--- Warehouse Logs ---"
    docker logs "$WAREHOUSE_CONTAINER_ID" | tail -n 200
    echo "--- Order Service Logs ---"
    docker logs "$ORDER_CONTAINER_ID" | tail -n 200
    echo "--- Loyalty Service Logs ---"
    docker logs "$LOYALTY_CONTAINER_ID" | tail -n 200
    exit 1
else
    echo "✅ Functional Validation Passed."
fi

echo
echo "All validations passed successfully!"
