#!/bin/bash
echo "--- Pruning unused Docker networks ---"
echo "This will remove all custom networks not used by at least one container."
docker network prune -f

echo "--- Pruning stopped containers ---"
docker container prune -f

echo "Done. You can now try running ./run-webshop.sh again."
