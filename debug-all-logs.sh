#!/bin/bash

# Redirect all output to log file and stdout
mkdir -p logs
exec > >(tee -a logs/debug-all-logs.log) 2>&1

echo "--- Webshop Logs ---"
kubectl logs -n webshop -l app=webshop --tail=50

echo
echo "--- Product Management Logs ---"
kubectl logs -n product-management -l app=product-management --tail=50

echo
echo "--- Warehouse Logs ---"
kubectl logs -n warehouse -l app=warehouse --tail=50

echo
echo "--- Order Service Logs ---"
kubectl logs -n order-service -l app=order-service --tail=50
