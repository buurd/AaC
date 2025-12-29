#!/bin/bash
set -e
clear

START_TIME=$(date +%s)

# Redirect all output to log file and stdout (overwrite)
exec > >(tee logs/validate-architecture.log) 2>&1

# --- Pre-Cleanup ---
# Remove any leftover containers from previous runs or run-webshop.sh to avoid conflicts
echo "--- Checking for leftover containers ---"
# List of container names used in this script and run-webshop.sh
CONTAINERS="webshop-demo pm-demo warehouse-demo order-service keycloak reverse-proxy db_webshop db_pm db_warehouse db_order db-webshop db-pm db-warehouse db-order loki promtail grafana"

for container in $CONTAINERS; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "Removing leftover container: $container"
        docker stop $container > /dev/null 2>&1 || true
        docker rm $container > /dev/null 2>&1 || true
    fi
done

# --- Cleanup ---
# This function will be called on script exit to ensure temporary files are removed.
cleanup() {
    echo
    echo "--- Cleaning up temporary files ---"
    rm -f workspace.json project-files.json requirements.json implementation-input.json code-structure-input.json code-structure-files.json relation-input.json test-content-input.json test-files.json
    rm -f structurizr/workspace.json

    echo "--- Stopping containers ---"
    # Stop containers started by this script (using IDs if available, or names)
    if [ -n "$WEBSHOP_CONTAINER_ID" ]; then docker stop "$WEBSHOP_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$PM_CONTAINER_ID" ]; then docker stop "$PM_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$WAREHOUSE_CONTAINER_ID" ]; then docker stop "$WAREHOUSE_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$ORDER_CONTAINER_ID" ]; then docker stop "$ORDER_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$KEYCLOAK_CONTAINER_ID" ]; then docker stop "$KEYCLOAK_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$PROXY_CONTAINER_ID" ]; then docker stop "$PROXY_CONTAINER_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$DB_WEBSHOP_ID" ]; then docker stop "$DB_WEBSHOP_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$DB_PM_ID" ]; then docker stop "$DB_PM_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$DB_WAREHOUSE_ID" ]; then docker stop "$DB_WAREHOUSE_ID" > /dev/null 2>&1 || true; fi
    if [ -n "$DB_ORDER_ID" ]; then docker stop "$DB_ORDER_ID" > /dev/null 2>&1 || true; fi

    # Also try to stop by name just in case
    docker stop webshop-demo pm-demo warehouse-demo order-service keycloak reverse-proxy db_webshop db_pm db_warehouse db_order > /dev/null 2>&1 || true

    # Remove network
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
# Function to run OPA validation and report results. Exits on failure.
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

# Function to run a runtime check using curl
run_runtime_check() {
    local title="$1"
    local url="$2"
    local expected_status="$3"
    local response_contains="$4"
    local container_id="$5"

    echo
    echo "--- Running: $title ---"
    echo "Checking URL: $url"

    # Check if container is still running
    if [ -n "$container_id" ]; then
        if ! docker ps -q --no-trunc | grep -q "$container_id"; then
             echo "🔴 Validation Failed: Container $container_id is not running."
             echo "--- Container Logs ---"
             docker logs "$container_id"
             exit 1
        fi
    fi

    # Temporarily disable set -e to capture curl failure
    set +e
    # Use -k to allow self-signed certs
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
    # Extract body (everything except the last line)
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
docker run --rm -v "$(pwd)/structurizr:/usr/local/structurizr" structurizr/cli export -workspace workspace.dsl -format json > /dev/null
mv -f structurizr/workspace.json workspace.json

echo "Step 2: Generating file system view..."
find . -path ./.git -prune -o -type f -print | sed 's|^\./||' | jq -R . | jq -s . > project-files.json

echo "Step 3: Aggregating requirements..."
docker run --rm -v "$(pwd):/workdir" mikefarah/yq eval-all -o=json '. as $doc ireduce ({}; .requirements += [$doc])' requirements/*.yaml > requirements.json

echo "Step 4: Combining inputs for validations..."
# Implementation Validation Input
jq -n '{files: $files, reqs: $reqs}' --slurpfile files project-files.json --slurpfile reqs requirements.json > implementation-input.json

# Code Structure Validation Input
find . -name "*.java" -print0 | xargs -0 -I {} jq -Rs --arg path "{}" '{$path: .}' {} | jq -s 'add' > code-structure-files.json
jq -n '{files: $files, reqs: $reqs}' --slurpfile files code-structure-files.json --slurpfile reqs requirements.json > code-structure-input.json

# Test Content Input (for HTTPS check)
find e2e-tests -name "*.js" -print0 | xargs -0 -I {} jq -Rs --arg path "{}" '{$path: .}' {} | jq -s 'add' > test-files.json
jq -n '{files: $files, reqs: $reqs}' --slurpfile files test-files.json --slurpfile reqs requirements.json > test-content-input.json

# Relation Validation Input (Model + Requirements)
jq -n '{model: $model, reqs: $reqs}' --slurpfile model workspace.json --slurpfile reqs requirements.json > relation-input.json

# --- Static Validations ---
run_opa_validation "Traceability Validation" "/project/requirements.json" "/project/policies/check_traceability.rego" "data.requirements.traceability.violation"
run_opa_validation "Relation Validation" "/project/relation-input.json" "/project/policies/check_relation.rego" "data.structurizr.relation.violation"
run_opa_validation "Container Validation" "/project/relation-input.json" "/project/policies/check_containers.rego" "data.structurizr.containers.violation"
run_opa_validation "Component Validation" "/project/workspace.json" "/project/policies/check_components.rego" "data.structurizr.components.violation"
run_opa_validation "Implementation Validation" "/project/implementation-input.json" "/project/policies/check_implementation.rego" "data.project_files.violation"
run_opa_validation "Code Structure Validation" "/project/code-structure-input.json" "/project/policies/check_class_structure.rego" "data.code.structure.violation"
run_opa_validation "Dependency Validation" "/project/code-structure-input.json" "/project/policies/check_dependency.rego" "data.code.dependency.violation"
run_opa_validation "HTTPS Usage Validation" "/project/test-content-input.json" "/project/policies/check_https_usage.rego" "data.security.https.violation"

# --- Contract Validation (Pact) ---
echo
echo "--- Running: Contract Validation (Pact) ---"

# Ensure pacts directory exists
mkdir -p pacts

# Define modules to scan
MODULES="webshop productManagementSystem warehouse orderService"

# 1. Run Consumer Tests (Generate Pacts)
echo "Phase 1: Generating Contracts (Consumer Tests)..."
for module in $MODULES; do
    if [ -d "$module" ] && [ -f "$module/pom.xml" ]; then
        echo "  Scanning $module for consumer tests..."
        # We use 'mvn test' but filter by tag 'pact-consumer'.
        # If no tests match, it might fail or pass depending on config, so we allow failure if no tests found but check output?
        # Better: Just run it. If it fails, it fails. If no tests, it passes (usually).
        if ! docker run --rm \
            -v "$(pwd)/$module:/usr/src/mymaven" \
            -v "$(pwd)/pacts:/usr/pacts" \
            -v "$(pwd)/m2-cache:/root/.m2" \
            -w /usr/src/mymaven \
            maven:3.9.6-eclipse-temurin-21 mvn clean test -Dgroups=pact-consumer -DfailIfNoTests=false; then
            echo "🔴 Consumer Contract Tests Failed in $module"
            exit 1
        fi
    fi
done

# 2. Run Provider Tests (Verify Pacts)
echo "Phase 2: Verifying Contracts (Provider Tests)..."
for module in $MODULES; do
    if [ -d "$module" ] && [ -f "$module/pom.xml" ]; then
        echo "  Scanning $module for provider tests..."
        if ! docker run --rm \
            -v "$(pwd)/$module:/usr/src/mymaven" \
            -v "$(pwd)/pacts:/usr/pacts" \
            -v "$(pwd)/m2-cache:/root/.m2" \
            -w /usr/src/mymaven \
            maven:3.9.6-eclipse-temurin-21 mvn clean test -Dgroups=pact-provider -DfailIfNoTests=false; then
            echo "🔴 Provider Contract Verification Failed in $module"
            exit 1
        fi
    fi
done

echo "✅ Contract Validation Passed."

# --- Runtime Validation ---
echo
echo "--- Preparing for Runtime Validations ---"
echo "Setting up Certificates..."
chmod +x infrastructure/setup-certs.sh
./infrastructure/setup-certs.sh

# Create a local Maven cache directory
mkdir -p ./m2-cache

echo "Compiling Webshop application..."
docker run --rm \
    -v "$(pwd)/webshop:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/root/.m2" \
    -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests

echo "Compiling Product Management application..."
docker run --rm \
    -v "$(pwd)/productManagementSystem:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/root/.m2" \
    -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests

echo "Compiling Warehouse Service..."
docker run --rm \
    -v "$(pwd)/warehouse:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/root/.m2" \
    -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests

echo "Compiling Order Service..."
docker run --rm \
    -v "$(pwd)/orderService:/usr/src/mymaven" \
    -v "$(pwd)/m2-cache:/root/.m2" \
    -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests

# Create a dedicated network for the containers
NETWORK_NAME="webshop-net-$$"
docker network create $NETWORK_NAME

# Start Webshop Database container
echo "Starting Webshop Database container..."
DB_WEBSHOP_ID=$(docker run -d --network $NETWORK_NAME --name db_webshop -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres postgres:14-alpine)
echo "Webshop Database container started with ID: $DB_WEBSHOP_ID"

# Start PM Database container
echo "Starting PM Database container..."
DB_PM_ID=$(docker run -d --network $NETWORK_NAME --name db_pm -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres postgres:14-alpine)
echo "PM Database container started with ID: $DB_PM_ID"

# Start Warehouse Database container
echo "Starting Warehouse Database container..."
DB_WAREHOUSE_ID=$(docker run -d --network $NETWORK_NAME --name db_warehouse -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres postgres:14-alpine)
echo "Warehouse Database container started with ID: $DB_WAREHOUSE_ID"

# Start Order Database container
echo "Starting Order Database container..."
DB_ORDER_ID=$(docker run -d --network $NETWORK_NAME --name db_order -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres postgres:14-alpine)
echo "Order Database container started with ID: $DB_ORDER_ID"

echo "Waiting for Databases to initialize (15s)..."
sleep 15

# Start Keycloak
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
echo "Keycloak container started with ID: $KEYCLOAK_CONTAINER_ID"

# Start Webshop container
echo "Starting Webshop server in a Docker container..."
# Named webshop-demo for PM to find it
WEBSHOP_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name webshop-demo \
    -v "$(pwd)/webshop/target:/app" \
    -e DB_URL=jdbc:postgresql://db_webshop:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e JWKS_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/certs \
    -e ISSUER_URL=https://reverse-proxy:8446/realms/webshop-realm \
    eclipse-temurin:21-jre java -jar /app/webshop-1.0-SNAPSHOT-jar-with-dependencies.jar)
echo "Webshop container started with ID: $WEBSHOP_CONTAINER_ID"

# Start Product Management container
echo "Starting Product Management server in a Docker container..."
# Fixed name to pm-demo to match nginx.conf
PM_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name pm-demo \
    -v "$(pwd)/productManagementSystem/target:/app" \
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
echo "Product Management container started with ID: $PM_CONTAINER_ID"

# Start Warehouse container
echo "Starting Warehouse Service in a Docker container..."
# Named warehouse-demo for PM to find it
WAREHOUSE_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name warehouse-demo \
    -v "$(pwd)/warehouse/target:/app" \
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
echo "Warehouse container started with ID: $WAREHOUSE_CONTAINER_ID"

# Start Order Service container
echo "Starting Order Service in a Docker container..."
ORDER_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name order-service \
    -v "$(pwd)/orderService/target:/app" \
    -e DB_URL=jdbc:postgresql://db_order:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e WAREHOUSE_RESERVE_URL=http://warehouse-demo:8002/api/stock/reserve \
    -e JWKS_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/certs \
    -e ISSUER_URL=https://reverse-proxy:8446/realms/webshop-realm \
    -e CLIENT_ID=order-client \
    -e CLIENT_SECRET=order-secret \
    -e TOKEN_URL=http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token \
    eclipse-temurin:21-jre java -jar /app/orderService-1.0-SNAPSHOT-jar-with-dependencies.jar)
echo "Order Service container started with ID: $ORDER_CONTAINER_ID"

echo "Waiting 2 seconds for Docker DNS to propagate..."
sleep 2

# Start Reverse Proxy
echo "Starting Reverse Proxy (Nginx)..."
PROXY_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name reverse-proxy \
    -p 8443:8443 -p 8444:8444 -p 8445:8445 -p 8446:8446 -p 8447:8447 \
    -v "$(pwd)/infrastructure/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" \
    -v "$(pwd)/infrastructure/nginx/certs:/etc/nginx/certs:ro" \
    nginx:alpine)
echo "Proxy container started with ID: $PROXY_CONTAINER_ID"

echo "Waiting for services to initialize (40s)..."
sleep 40

# Check if proxy is running
if ! docker ps -q --no-trunc | grep -q "$PROXY_CONTAINER_ID"; then
    echo "🔴 Reverse Proxy failed to start!"
    docker logs "$PROXY_CONTAINER_ID"
    exit 1
fi

# Run the actual runtime checks (via HTTPS Proxy)
# Note: We use -k to ignore self-signed cert errors in validation
run_runtime_check "Runtime Validation (REQ-005) - HTTPS" "https://localhost:8443/" "200" "" "$WEBSHOP_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-007) - HTTPS" "https://localhost:8443/products" "200" "The Hitchhiker's Guide to the Galaxy" "$WEBSHOP_CONTAINER_ID"
# PM and Warehouse are now SECURED. They should return 302 Found (Redirect to Login) if accessed without token.
run_runtime_check "Runtime Validation (REQ-014) - HTTPS (Secured)" "https://localhost:8444/products" "302" "" "$PM_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-031) - HTTPS (Secured)" "https://localhost:8445/products" "302" "" "$WAREHOUSE_CONTAINER_ID"
# Webshop is public, so it should still be 200.
run_runtime_check "Runtime Validation (REQ-034) - HTTPS (Secured)" "https://localhost:8445/products" "302" "" "$WAREHOUSE_CONTAINER_ID"
# Check Keycloak
run_runtime_check "Runtime Validation (Keycloak) - HTTPS" "https://localhost:8446/" "200" "Welcome to Keycloak" "$KEYCLOAK_CONTAINER_ID"
# Check Order Service
run_runtime_check "Runtime Validation (REQ-051) - HTTPS (Secured)" "https://localhost:8447/" "200" "Order Service" "$ORDER_CONTAINER_ID"

# --- Functional Validation (Cypress) ---
echo
echo "--- Running: Functional Validation (Cypress) ---"
echo "Running Cypress tests against https://reverse-proxy:8444..."

set +e
# Run Cypress in a container connected to the same network
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
    exit 1
else
    echo "✅ Functional Validation Passed."
fi

# --- Final Status ---
echo
echo "All validations passed successfully!"
