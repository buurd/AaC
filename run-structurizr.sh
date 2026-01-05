#!/bin/bash
set -e
clear

# Redirect all output to log file and stdout
mkdir -p logs
exec > >(tee -a logs/run-structurizr.log) 2>&1

if [ ! "$(docker ps -q -f name=structurizr)" ]; then
    if [ "$(docker ps -aq -f status=exited -f name=structurizr)" ]; then
        # cleanup
        docker rm structurizr
    fi

    echo "Starting Structurizr Lite container..."
    # We mount the structurizr directory to the container's expected workspace root.
    # The ADRs and documentation are now inside the structurizr/ directory, so relative paths work.
    docker run -it --rm -p 8080:8080 \
      -v "$(pwd)/structurizr:/usr/local/structurizr" \
      --name structurizr \
      structurizr/lite
fi
