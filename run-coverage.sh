#!/bin/bash
set -e

# Create logs directory if it doesn't exist
mkdir -p logs

# Redirect all output to log file and stdout
exec > >(tee -a logs/coverage.log) 2>&1

echo "--- Running Tests with Coverage ---"

# Ensure m2-cache exists
mkdir -p m2-cache

# Clean up stale pacts
rm -f pacts/WebshopService-OrderService.json

# Download JaCoCo CLI to cache so it's available for aggregation
echo "Downloading JaCoCo CLI..."
docker run --rm \
    -u "$(id -u):$(id -g)" \
    -e HOME=/tmp \
    -e MAVEN_CONFIG=/tmp/.m2 \
    -v "$(pwd)/m2-cache:/tmp/.m2" \
    -v /etc/passwd:/etc/passwd:ro \
    -v /etc/group:/etc/group:ro \
    maven:3.9.6-eclipse-temurin-21 \
    mvn dependency:get -Dartifact=org.jacoco:org.jacoco.cli:0.8.11:jar:nodeps -Dmaven.repo.local=/tmp/.m2/repository

MODULES="productManagementSystem orderService warehouse webshop loyaltyService"

for module in $MODULES; do
    echo "Running tests for $module..."
    docker run --rm \
        -u "$(id -u):$(id -g)" \
        -e HOME=/tmp \
        -e MAVEN_CONFIG=/tmp/.m2 \
        -v "$(pwd)/applications/$module:/usr/src/mymaven" \
        -v "$(pwd)/m2-cache:/tmp/.m2" \
        -v "$(pwd)/pacts:/usr/src/pacts" \
        -v /etc/passwd:/etc/passwd:ro \
        -v /etc/group:/etc/group:ro \
        -w /usr/src/mymaven \
        maven:3.9.6-eclipse-temurin-21 mvn clean test jacoco:report -Dmaven.repo.local=/tmp/.m2/repository

    echo "Coverage report for $module generated at: applications/$module/target/site/jacoco/index.html"
done

echo "--- Aggregating Coverage Reports ---"

# Create a directory for the aggregated report
mkdir -p coverage-aggregate

# Construct arguments for jacoco-cli
JACOCO_ARGS=""

for module in $MODULES; do
    if [ -f "applications/$module/target/jacoco.exec" ]; then
        JACOCO_ARGS="$JACOCO_ARGS applications/$module/target/jacoco.exec"
        JACOCO_ARGS="$JACOCO_ARGS --classfiles applications/$module/target/classes"
        JACOCO_ARGS="$JACOCO_ARGS --sourcefiles applications/$module/src/main/java"
    fi
done

if [ -n "$JACOCO_ARGS" ]; then
    echo "Generating aggregated report..."

    # Run jacoco-cli inside docker
    # Mount root project to /project to access all modules
    # Mount m2-cache to access the downloaded jacoco-cli jar
    docker run --rm \
        -u "$(id -u):$(id -g)" \
        -e HOME=/tmp \
        -v "$(pwd):/project" \
        -v "$(pwd)/m2-cache:/tmp/.m2" \
        -v /etc/passwd:/etc/passwd:ro \
        -v /etc/group:/etc/group:ro \
        -w /project \
        maven:3.9.6-eclipse-temurin-21 \
        java -jar /tmp/.m2/repository/org/jacoco/org.jacoco.cli/0.8.11/org.jacoco.cli-0.8.11-nodeps.jar \
        report $JACOCO_ARGS \
        --html coverage-aggregate/index.html \
        --name "Good Project Aggregated Coverage"

    echo "Aggregated coverage report generated at: coverage-aggregate/index.html"
else
    echo "No coverage data found to aggregate."
fi

echo "--- Coverage Reports Generated ---"
