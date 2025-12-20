#!/bin/bash
set -e
clear

# --- Cleanup ---
# This function will be called on script exit to ensure temporary files are removed.
cleanup() {
    echo
    echo "--- Cleaning up temporary files ---"
    rm -f workspace.json project-files.json requirements.json implementation-input.json code-structure-input.json code-structure-files.json relation-input.json
    rm -f structurizr/workspace.json

    # Stop and remove the containers
    if [ -n "$WEBSHOP_CONTAINER_ID" ]; then
        echo "Stopping Webshop container ($WEBSHOP_CONTAINER_ID)..."
        docker stop $WEBSHOP_CONTAINER_ID > /dev/null 2>&1 || true
        docker rm $WEBSHOP_CONTAINER_ID > /dev/null 2>&1 || true
    fi
    if [ -n "$PM_CONTAINER_ID" ]; then
        echo "Stopping Product Management container ($PM_CONTAINER_ID)..."
        docker stop $PM_CONTAINER_ID > /dev/null 2>&1 || true
        docker rm $PM_CONTAINER_ID > /dev/null 2>&1 || true
    fi
    if [ -n "$WAREHOUSE_CONTAINER_ID" ]; then
        echo "Stopping Warehouse container ($WAREHOUSE_CONTAINER_ID)..."
        docker stop $WAREHOUSE_CONTAINER_ID > /dev/null 2>&1 || true
        docker rm $WAREHOUSE_CONTAINER_ID > /dev/null 2>&1 || true
    fi
    if [ -n "$DB_WEBSHOP_ID" ]; then
        echo "Stopping Webshop Database container ($DB_WEBSHOP_ID)..."
        docker stop $DB_WEBSHOP_ID > /dev/null 2>&1 || true
        docker rm $DB_WEBSHOP_ID > /dev/null 2>&1 || true
    fi
    if [ -n "$DB_PM_ID" ]; then
        echo "Stopping PM Database container ($DB_PM_ID)..."
        docker stop $DB_PM_ID > /dev/null 2>&1 || true
        docker rm $DB_PM_ID > /dev/null 2>&1 || true
    fi
    if [ -n "$DB_WAREHOUSE_ID" ]; then
        echo "Stopping Warehouse Database container ($DB_WAREHOUSE_ID)..."
        docker stop $DB_WAREHOUSE_ID > /dev/null 2>&1 || true
        docker rm $DB_WAREHOUSE_ID > /dev/null 2>&1 || true
    fi
    if [ -n "$NETWORK_NAME" ]; then
        echo "Removing Docker network ($NETWORK_NAME)..."
        docker network rm $NETWORK_NAME > /dev/null 2>&1 || true
    fi
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
    RESPONSE=$(docker run --rm --network="host" curlimages/curl:7.78.0 curl -v -s -w "\\n%{http_code}" "$url" 2>&1)
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

# --- Runtime Validation ---
echo
echo "--- Preparing for Runtime Validations ---"
echo "Compiling Webshop application..."
docker run --rm -v "$(pwd)/webshop:/usr/src/mymaven" -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package > /dev/null

echo "Compiling Product Management application..."
docker run --rm -v "$(pwd)/productManagementSystem:/usr/src/mymaven" -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package > /dev/null

echo "Compiling Warehouse Service..."
docker run --rm -v "$(pwd)/warehouse:/usr/src/mymaven" -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package > /dev/null

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

echo "Waiting for Databases to initialize (15s)..."
sleep 15

# Start Webshop container
echo "Starting Webshop server in a Docker container..."
# Named webshop-demo for PM to find it
WEBSHOP_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name webshop-demo -p 8000:8000 \
    -v "$(pwd)/webshop/target:/app" \
    -e DB_URL=jdbc:postgresql://db_webshop:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    eclipse-temurin:21-jre java -jar /app/webshop-1.0-SNAPSHOT-jar-with-dependencies.jar)
echo "Webshop container started with ID: $WEBSHOP_CONTAINER_ID"

# Start Product Management container
echo "Starting Product Management server in a Docker container..."
PM_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name pm-system -p 8001:8001 \
    -v "$(pwd)/productManagementSystem/target:/app" \
    -e DB_URL=jdbc:postgresql://db_pm:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e WEBSHOP_API_URL=http://webshop-demo:8000/api/products/sync \
    -e WAREHOUSE_API_URL=http://warehouse-demo:8002/api/products/sync \
    eclipse-temurin:21-jre java -jar /app/productManagementSystem-1.0-SNAPSHOT-jar-with-dependencies.jar)
echo "Product Management container started with ID: $PM_CONTAINER_ID"

# Start Warehouse container
echo "Starting Warehouse Service in a Docker container..."
# Named warehouse-demo for PM to find it
WAREHOUSE_CONTAINER_ID=$(docker run -d --network $NETWORK_NAME --name warehouse-demo -p 8002:8002 \
    -v "$(pwd)/warehouse/target:/app" \
    -e DB_URL=jdbc:postgresql://db_warehouse:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e WEBSHOP_STOCK_API_URL=http://webshop-demo:8000/api/stock/sync \
    eclipse-temurin:21-jre java -jar /app/warehouse-1.0-SNAPSHOT-jar-with-dependencies.jar)
echo "Warehouse container started with ID: $WAREHOUSE_CONTAINER_ID"

echo "Waiting for services to initialize (5s)..."
sleep 5

# Run the actual runtime checks
run_runtime_check "Runtime Validation (REQ-005)" "http://localhost:8000/" "200" "" "$WEBSHOP_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-007)" "http://localhost:8000/products" "200" "The Hitchhiker's Guide to the Galaxy" "$WEBSHOP_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-014)" "http://localhost:8001/" "200" "" "$PM_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-015)" "http://localhost:8001/products" "200" "Hammer" "$PM_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-016)" "http://localhost:8001/products/create" "200" "" "$PM_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-017)" "http://localhost:8001/products/edit" "200" "" "$PM_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-018)" "http://localhost:8001/products/delete" "200" "" "$PM_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-031)" "http://localhost:8002/" "200" "Warehouse Service" "$WAREHOUSE_CONTAINER_ID"
run_runtime_check "Runtime Validation (REQ-034)" "http://localhost:8002/products" "200" "Sample Product" "$WAREHOUSE_CONTAINER_ID"

# --- Functional Validation (Cypress) ---
echo
echo "--- Running: Functional Validation (Cypress) ---"
echo "Running Cypress tests against http://pm-system:8001..."

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
    exit 1
else
    echo "✅ Functional Validation Passed."
fi

# --- Final Status ---
echo
echo "All validations passed successfully!"
