#!/bin/bash
set -e
clear

# --- Cleanup ---
# This function will be called on script exit to ensure temporary files are removed.
cleanup() {
    echo
    echo "--- Cleaning up temporary files ---"
    rm -f workspace.json project-files.json requirements.json implementation-input.json code-structure-input.json code-structure-files.json
    rm -f structurizr/workspace.json

    # Stop the containers if they are running
    if [ -n "$WEBSHOP_CONTAINER_ID" ]; then
        echo "Stopping Webshop container ($WEBSHOP_CONTAINER_ID)..."
        docker stop $WEBSHOP_CONTAINER_ID > /dev/null 2>&1 || true
    fi
    if [ -n "$DB_CONTAINER_ID" ]; then
        echo "Stopping Database container ($DB_CONTAINER_ID)..."
        docker stop $DB_CONTAINER_ID > /dev/null 2>&1 || true
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

    echo
    echo "--- Running: $title ---"
    echo "Checking URL: $url"

    RESPONSE=$(docker run --rm --network="host" curlimages/curl:7.78.0 curl -s -w "\\n%{http_code}" "$url")
    HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
    HTTP_BODY=$(echo "$RESPONSE" | sed '$d')

    echo "Server returned status: $HTTP_STATUS"

    if [ "$HTTP_STATUS" -ne "$expected_status" ]; then
        echo "🔴 Validation Failed: Expected status $expected_status, got $HTTP_STATUS"
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

echo "Step 4: Combining inputs for implementation validation..."
jq -n '{files: $files, reqs: $reqs}' --slurpfile files project-files.json --slurpfile reqs requirements.json > implementation-input.json

echo "Step 5: Creating file content map for code structure validation..."
find . -name "*.java" -print0 | xargs -0 -I {} jq -Rs --arg path "{}" '{$path: .}' {} | jq -s 'add' > code-structure-files.json
jq -n '{files: $files, reqs: $reqs}' --slurpfile files code-structure-files.json --slurpfile reqs requirements.json > code-structure-input.json

# --- Static Validations ---
run_opa_validation "Traceability Validation" "/project/requirements.json" "/project/policies/check_traceability.rego" "data.requirements.traceability.violation"
run_opa_validation "Relation Validation" "/project/workspace.json" "/project/policies/check_relation.rego" "data.structurizr.violation"
run_opa_validation "Container Validation" "/project/workspace.json" "/project/policies/check_containers.rego" "data.structurizr.containers.violation"
run_opa_validation "Component Validation" "/project/workspace.json" "/project/policies/check_components.rego" "data.structurizr.components.violation"
run_opa_validation "Implementation Validation" "/project/implementation-input.json" "/project/policies/check_implementation.rego" "data.project_files.violation"
run_opa_validation "Code Structure Validation" "/project/code-structure-input.json" "/project/policies/check_class_structure.rego" "data.code.structure.violation"

# --- Runtime Validation ---
echo
echo "--- Preparing for Runtime Validations ---"
echo "Compiling Webshop application..."
docker run --rm -v "$(pwd)/webshop:/usr/src/mymaven" -w /usr/src/mymaven maven:3.9.6-eclipse-temurin-21 mvn clean package > /dev/null

# Create a dedicated network for the containers
NETWORK_NAME="webshop-net-$$"
docker network create $NETWORK_NAME

# Start PostgreSQL container
echo "Starting Database container..."
DB_CONTAINER_ID=$(docker run -d --rm --network $NETWORK_NAME --name db_host -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres postgres:14-alpine)
echo "Database container started with ID: $DB_CONTAINER_ID"

# Start Webshop container
echo "Starting Webshop server in a Docker container..."
# We now use the Docker network to communicate, not host networking
WEBSHOP_CONTAINER_ID=$(docker run -d --rm --network $NETWORK_NAME -p 8000:8000 \
    -v "$(pwd)/webshop/target:/app" \
    -e DB_URL=jdbc:postgresql://db_host:5432/postgres \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    eclipse-temurin:21-jre java -jar /app/webshop-1.0-SNAPSHOT-jar-with-dependencies.jar)
echo "Webshop container started with ID: $WEBSHOP_CONTAINER_ID"

echo "Waiting for services to initialize..."
# Wait for Postgres to be ready
sleep 10
# Wait for Webshop to be ready
sleep 5

# Run the actual runtime checks
run_runtime_check "Runtime Validation (REQ-005)" "http://localhost:8000/" "200"
run_runtime_check "Runtime Validation (REQ-007)" "http://localhost:8000/products" "200" "The Hitchhiker's Guide to the Galaxy"

# --- Final Status ---
echo
echo "All validations passed successfully!"
