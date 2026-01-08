#!/bin/bash

# Redirect all output to log file and stdout
mkdir -p logs
exec > >(tee -a logs/debug-endpoints.log) 2>&1

echo "--- Webshop Endpoints ---"
kubectl get endpoints -n webshop webshop

echo
echo "--- PM Endpoints ---"
kubectl get endpoints -n product-management product-management

echo
echo "--- Warehouse Endpoints ---"
kubectl get endpoints -n warehouse warehouse

echo
echo "--- Order Service Endpoints ---"
kubectl get endpoints -n order-service order-service

echo
echo "--- Keycloak Endpoints ---"
kubectl get endpoints -n infrastructure keycloak

echo
echo "--- Grafana Endpoints ---"
kubectl get endpoints -n infrastructure grafana
