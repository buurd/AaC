#!/bin/bash
# set -e
clear

echo "--- DEBUG SCRIPT ---"
echo "This script will leave temporary files for inspection."
echo

# --- Pre-computation Steps ---
echo "Step 1: Exporting Structurizr DSL to JSON..."
docker run --rm -v "$(pwd)/structurizr:/usr/local/structurizr" structurizr/cli export -workspace workspace.dsl -format json > /dev/null

# Move the generated file to the root for consistency, forcing overwrite
mv -f structurizr/workspace.json workspace.json

echo
echo "--- DEBUG: Content of workspace.json ---"
cat workspace.json
echo "----------------------------------------"
echo

# --- OPA Container Validation (Debug Mode) ---
echo "--- Running: Container Validation (Debug) ---"

# Run OPA with verbose logging to see how it evaluates the policy
# We capture stdout and stderr
echo "Executing OPA command with --explain=full..."
docker run --rm -v "$(pwd):/project" openpolicyagent/opa eval \
    --explain=full \
    -i /project/workspace.json \
    -d /project/policies/check_containers.rego \
    "data.structurizr.containers.violation"

EXIT_CODE=$?
echo "OPA Exit Code: $EXIT_CODE"

echo
echo "Debug script finished. You can now inspect workspace.json."
