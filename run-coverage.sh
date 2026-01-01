#!/bin/bash
set -e

echo "--- Running Tests with Coverage ---"

MODULES="webshop productManagementSystem warehouse orderService"

for module in $MODULES; do
    echo "Running tests for $module..."
    docker run --rm \
        -v "$(pwd)/applications/$module:/usr/src/mymaven" \
        -v "$(pwd)/m2-cache:/root/.m2" \
        -w /usr/src/mymaven \
        maven:3.9.6-eclipse-temurin-21 mvn clean test jacoco:report

    echo "Coverage report for $module generated at: applications/$module/target/site/jacoco/index.html"
done

echo "--- Coverage Reports Generated ---"
