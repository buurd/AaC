#!/bin/bash

# Redirect all output to log file and stdout
mkdir -p logs
exec > >(tee -a logs/debug-loki.log) 2>&1

echo "--- Loki Pod Status ---"
kubectl get pods -n infrastructure -l app=loki

echo
echo "--- Loki Logs ---"
kubectl logs -n infrastructure -l app=loki --tail=100

echo
echo "--- Describe Loki Pod ---"
kubectl describe pod -n infrastructure -l app=loki
