#!/bin/bash
set -e

echo "--- Starting Vulnerability Scan ---"

# Ensure logs directory exists
mkdir -p logs
REPORT_FILE="logs/trivy-report.txt"
echo "Trivy Vulnerability Report - $(date)" > "$REPORT_FILE"
echo "----------------------------------------" >> "$REPORT_FILE"

# Check if trivy is installed
USE_DOCKER=0
if ! command -v trivy &> /dev/null; then
    echo "Trivy is not installed. Installing Trivy..."
    if command -v docker &> /dev/null; then
        echo "Using Trivy via Docker..."
        USE_DOCKER=1
    else
        echo "🔴 Error: Trivy is not installed and Docker is not available."
        exit 1
    fi
else
    USE_DOCKER=0
fi

# Global flag to track if any critical vulnerabilities were found
CRITICAL_FOUND=0

run_trivy_scan() {
    local type="$1"
    local target="$2"

    echo "Scanning $target..."
    echo "Scanning $target..." >> "$REPORT_FILE"

    # 1. Run full scan (CRITICAL,HIGH,MEDIUM,LOW,UNKNOWN) for the report
    # We don't fail here, just capture output
    set +e
    if [ "$USE_DOCKER" -eq 1 ]; then
        local cmd=(docker run --rm -v "/var/run/docker.sock:/var/run/docker.sock" -v "$(pwd):/project" -v "$HOME/.cache/trivy:/root/.cache/trivy" aquasec/trivy:latest "$type" --severity CRITICAL,HIGH,MEDIUM,LOW,UNKNOWN --no-progress "$target")
        "${cmd[@]}" >> "$REPORT_FILE" 2>&1
    else
        trivy "$type" --severity CRITICAL,HIGH,MEDIUM,LOW,UNKNOWN --no-progress "$target" >> "$REPORT_FILE" 2>&1
    fi
    set -e

    # 2. Check for CRITICALs specifically to set the flag
    # We run a quiet scan just to check exit code
    set +e
    if [ "$USE_DOCKER" -eq 1 ]; then
        local cmd_check=(docker run --rm -v "/var/run/docker.sock:/var/run/docker.sock" -v "$(pwd):/project" -v "$HOME/.cache/trivy:/root/.cache/trivy" aquasec/trivy:latest "$type" --severity CRITICAL --exit-code 1 --quiet --no-progress "$target")
        "${cmd_check[@]}" > /dev/null 2>&1
    else
        trivy "$type" --severity CRITICAL --exit-code 1 --quiet --no-progress "$target" > /dev/null 2>&1
    fi
    EXIT_CODE=$?
    set -e

    if [ $EXIT_CODE -ne 0 ]; then
        echo "  🔴 Critical Vulnerabilities found!"
        CRITICAL_FOUND=1
    else
        echo "  ✅ No Critical vulnerabilities."
    fi
    echo "----------------------------------------" >> "$REPORT_FILE"
}

# --- 1. Scan Container Images ---
echo "--- Scanning Container Images ---" | tee -a "$REPORT_FILE"
IMAGES=(
    "grafana/loki:3.3.2"
    "grafana/promtail:3.3.3"
    "grafana/grafana:latest"
    "postgres:15"
    "quay.io/keycloak/keycloak:24.0.0"
    "busybox:1.28"
    "eclipse-temurin:21-jre"
    "postgres:14-alpine"
    "nginx:alpine"
)

for img in "${IMAGES[@]}"; do
    run_trivy_scan "image" "$img"
done

# --- 2. Scan Application Dependencies (Source Code) ---
echo
echo "--- Scanning Application Dependencies ---" | tee -a "$REPORT_FILE"
MODULES="webshop productManagementSystem warehouse orderService loyaltyService"

for module in $MODULES; do
    if [ -d "applications/$module" ]; then
        if [ "$USE_DOCKER" -eq 1 ]; then
             run_trivy_scan "fs" "/project/applications/$module"
        else
             run_trivy_scan "fs" "applications/$module"
        fi
    fi
done

# --- 3. Scan E2E Tests Dependencies ---
if [ -d "e2e-tests" ]; then
    echo
    echo "Scanning E2E Tests Dependencies..." | tee -a "$REPORT_FILE"
    if [ "$USE_DOCKER" -eq 1 ]; then
         run_trivy_scan "fs" "/project/e2e-tests"
    else
         run_trivy_scan "fs" "e2e-tests"
    fi
fi

echo
echo "Scan complete. Full report available at $REPORT_FILE"

if [ $CRITICAL_FOUND -ne 0 ]; then
    echo "🔴 Critical vulnerabilities were detected!"
    exit 1
else
    echo "✅ No critical vulnerabilities found."
    exit 0
fi
