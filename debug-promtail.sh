#!/bin/bash

# Redirect all output to log file and stdout
mkdir -p logs
exec > >(tee -a logs/debug-promtail.log) 2>&1

echo "--- Promtail DaemonSet Status ---"
kubectl get daemonset -n infrastructure promtail

echo
echo "--- Promtail Pod Status ---"
kubectl get pods -n infrastructure -l app=promtail -o wide

echo
echo "--- Promtail ConfigMap ---"
kubectl get configmap -n infrastructure promtail-config -o yaml

echo
echo "--- Promtail Logs ---"
kubectl logs -n infrastructure -l app=promtail --tail=100

echo
echo "--- Promtail Positions (Tail) ---"
# Check if we can read the positions file to see what it's tracking
POD_NAME=$(kubectl get pods -n infrastructure -l app=promtail -o jsonpath="{.items[0].metadata.name}")
if [ -n "$POD_NAME" ]; then
    kubectl exec -n infrastructure $POD_NAME -- tail -n 20 /tmp/positions.yaml || echo "Could not read positions file"
else
    echo "No Promtail pod found."
fi

echo
echo "--- Describe Promtail Pod ---"
kubectl describe pod -n infrastructure -l app=promtail
