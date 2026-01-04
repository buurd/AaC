#!/bin/bash

# Redirect all output to log file and stdout
mkdir -p logs
exec > >(tee -a logs/debug-app-logs.log) 2>&1

echo "--- Webshop Logs ---"
kubectl logs -n webshop -l app=webshop --tail=200 --prefix=true

echo
echo "--- Product Management Logs ---"
kubectl logs -n product-management -l app=product-management --tail=200 --prefix=true

echo
echo "--- Loki Logs ---"
kubectl logs -n infrastructure -l app=loki --tail=200 --prefix=true
