#!/bin/bash
set -e
clear

echo "--- DEBUG RELATION SCRIPT ---"

# --- Pre-computation Steps ---
echo "Step 1: Exporting Structurizr DSL to JSON..."
docker run --rm -v "$(pwd)/structurizr:/usr/local/structurizr" structurizr/cli export -workspace workspace.dsl -format json > /dev/null
mv -f structurizr/workspace.json workspace.json

echo "Step 2: Aggregating requirements..."
docker run --rm -v "$(pwd):/workdir" mikefarah/yq eval-all -o=json '. as $doc ireduce ({}; .requirements += [$doc])' requirements/*.yaml > requirements.json

echo "Step 3: Creating relation input..."
jq -n '{model: $model, reqs: $reqs}' --slurpfile model workspace.json --slurpfile reqs requirements.json > relation-input.json

echo
echo "--- DEBUG: Content of relation-input.json (first 100 lines) ---"
head -n 100 relation-input.json
echo "-------------------------------------------------------------"

echo
echo "--- Running OPA with --explain=full ---"
docker run --rm -v "$(pwd):/project" openpolicyagent/opa eval \
    --explain=full \
    -i /project/relation-input.json \
    -d /project/policies/check_relation.rego \
    "data.structurizr.relation.violation"

echo
echo "Debug script finished."
