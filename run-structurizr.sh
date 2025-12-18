#!/bin/bash
set -e
clear

if [ ! "$(docker ps -q -f name=structurizr)" ]; then
    if [ "$(docker ps -aq -f status=exited -f name=structurizr)" ]; then
        # cleanup
        docker rm structurizr
    fi
    # run
    docker run -it --rm -p 8080:8080 -v "$(pwd)/structurizr:/usr/local/structurizr" --name structurizr structurizr/lite
fi