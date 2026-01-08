#!/bin/bash

clear

# Redirect all output to log file and stdout
mkdir -p logs
exec > >(tee -a logs/debug-k8s-status.log) 2>&1

echo "--- Pod Status (All Namespaces) ---"
kubectl get pods -A -o wide

echo
echo "--- Non-Running or Not-Ready Pods ---"
# Get all pods, filter for those not Running or not fully Ready (e.g. 0/1)
kubectl get pods -A --no-headers | awk '$3 != "Running" || $2 !~ /^([0-9]+)\/\1$/ {print $0}'

echo
echo "--- Describe Suspicious Pods ---"
# Describe pods that are not Running or not Ready
kubectl get pods -A --no-headers | awk '$3 != "Running" || $2 !~ /^([0-9]+)\/\1$/ {print "-n " $1 " " $2}' | xargs -r -L 1 kubectl describe pod

echo
echo "--- PV/PVC Status ---"
kubectl get pv
kubectl get pvc -A

echo
echo "--- Service Endpoints ---"
kubectl get endpoints -A

echo
echo "--- Ingress Status ---"
kubectl get ingress -A

echo
echo "--- Recent Events ---"
kubectl get events -A --sort-by='.lastTimestamp' | tail -n 30
