#!/bin/bash

clear

# Redirect all output to log file and stdout
mkdir -p logs
exec > >(tee -a logs/debug-connectivity.log) 2>&1

echo "--- Starting Connectivity Check ---"

# Get IP of Webshop Pod
WEBSHOP_POD_IP=$(kubectl get pod -n webshop -l app=webshop -o jsonpath="{.items[0].status.podIP}")
echo "Webshop Pod IP: $WEBSHOP_POD_IP"

if [ -z "$WEBSHOP_POD_IP" ]; then
    echo "Error: Could not find Webshop Pod IP."
else
    # Run a temporary curl pod
    echo "Attempting to access Webshop directly via Pod IP..."
    kubectl run curl-test-pod --image=curlimages/curl --restart=Never --rm -i --tty -- \
        curl -v --connect-timeout 5 http://$WEBSHOP_POD_IP:8000/
fi

echo
echo "--- Checking Service DNS ---"
kubectl run dns-test-pod --image=busybox:1.28 --restart=Never --rm -i --tty -- \
    nslookup webshop.webshop.svc.cluster.local

echo
echo "--- Checking Access via Service ---"
kubectl run curl-svc-test-pod --image=curlimages/curl --restart=Never --rm -i --tty -- \
    curl -v --connect-timeout 5 http://webshop.webshop.svc.cluster.local:80/

echo
echo "--- Checking Grafana Deployment ---"
kubectl get deployment -n infrastructure grafana -o yaml || echo "Grafana Deployment not found"
