#!/bin/bash

clear

# Redirect all output to log file and stdout
mkdir -p logs
exec > >(tee -a logs/debug-k8s.log) 2>&1

echo "--- Kubernetes Cluster Status ---"
kubectl get nodes

echo
echo "--- All Pods ---"
kubectl get pods --all-namespaces

echo
echo "--- All Services ---"
kubectl get services --all-namespaces

echo
echo "--- All Ingresses ---"
kubectl get ingress --all-namespaces

echo
echo "--- Describe Ingress Controller ---"
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx --tail=50

echo
echo "--- Describe Webshop Pod (if exists) ---"
kubectl describe pod -n webshop -l app=webshop
