#!/bin/bash

clear
# Redirect all output to log file and stdout
mkdir -p logs
exec > >(tee -a logs/debug-storage.log) 2>&1

check_db_storage() {
    local SERVICE_NAME=$1
    local NAMESPACE=$2
    local APP_LABEL=$3
    local PV_NAME=$4

    echo "--- Checking $SERVICE_NAME Content (Inside Pod) ---"
    # Get the pod name
    POD_NAME=$(kubectl get pod -n $NAMESPACE -l app=$APP_LABEL -o jsonpath="{.items[0].metadata.name}")

    if [ -n "$POD_NAME" ]; then
        echo "Pod found: $POD_NAME"
        echo "Listing /var/lib/postgresql/data inside the container:"
        # List files to verify DB is initialized and writing data
        kubectl exec -n $NAMESPACE $POD_NAME -- ls -F /var/lib/postgresql/data
    else
        echo "Error: $SERVICE_NAME Pod not found."
    fi

    echo
    echo "--- Checking Persistent Volume (PV) Config for $SERVICE_NAME ---"
    # Show the hostPath configuration
    if kubectl get pv $PV_NAME > /dev/null 2>&1; then
        kubectl get pv $PV_NAME -o yaml | grep -A 2 "hostPath:"
    else
        echo "Error: PV $PV_NAME not found."
    fi
    echo
}

check_loki_storage() {
    local SERVICE_NAME="Loki"
    local NAMESPACE="infrastructure"
    local APP_LABEL="loki"
    local PV_NAME="loki-pv"

    echo "--- Checking $SERVICE_NAME Content (Inside Pod) ---"
    # Get the pod name
    POD_NAME=$(kubectl get pod -n $NAMESPACE -l app=$APP_LABEL -o jsonpath="{.items[0].metadata.name}")

    if [ -n "$POD_NAME" ]; then
        echo "Pod found: $POD_NAME"
        echo "Listing /loki inside the container:"
        # List files to verify Loki is initialized and writing data
        # Explicitly specify container name 'loki' to avoid ambiguity
        kubectl exec -n $NAMESPACE $POD_NAME -c loki -- ls -F /loki
    else
        echo "Error: $SERVICE_NAME Pod not found."
    fi

    echo
    echo "--- Checking Persistent Volume (PV) Config for $SERVICE_NAME ---"
    # Show the hostPath configuration
    if kubectl get pv $PV_NAME > /dev/null 2>&1; then
        kubectl get pv $PV_NAME -o yaml | grep -A 2 "hostPath:"
    else
        echo "Error: PV $PV_NAME not found."
    fi
    echo
}

check_grafana_storage() {
    local SERVICE_NAME="Grafana"
    local NAMESPACE="infrastructure"
    local APP_LABEL="grafana"
    local PV_NAME="grafana-pv"

    echo "--- Checking $SERVICE_NAME Content (Inside Pod) ---"
    # Get the pod name
    POD_NAME=$(kubectl get pod -n $NAMESPACE -l app=$APP_LABEL -o jsonpath="{.items[0].metadata.name}")

    if [ -n "$POD_NAME" ]; then
        echo "Pod found: $POD_NAME"
        echo "Listing /var/lib/grafana inside the container:"
        # List files to verify Grafana is initialized and writing data
        kubectl exec -n $NAMESPACE $POD_NAME -- ls -F /var/lib/grafana
    else
        echo "Error: $SERVICE_NAME Pod not found."
    fi

    echo
    echo "--- Checking Persistent Volume (PV) Config for $SERVICE_NAME ---"
    # Show the hostPath configuration
    if kubectl get pv $PV_NAME > /dev/null 2>&1; then
        kubectl get pv $PV_NAME -o yaml | grep -A 2 "hostPath:"
    else
        echo "Error: PV $PV_NAME not found."
    fi
    echo
}

check_db_storage "Webshop DB" "webshop" "webshop-db" "webshop-db-pv"
check_db_storage "Product Management DB" "product-management" "pm-db" "pm-db-pv"
check_db_storage "Warehouse DB" "warehouse" "warehouse-db" "warehouse-db-pv"
check_db_storage "Order Service DB" "order-service" "order-db" "order-db-pv"
check_db_storage "Keycloak DB" "infrastructure" "keycloak-db" "keycloak-db-pv"
check_loki_storage
check_grafana_storage

echo "--- Diagnosis ---"
echo "1. If you see files listed above (like 'base/', 'global/', 'pg_wal/') inside the pods,"
echo "   it means the Databases are working correctly and storing data inside Minikube."
echo
echo "2. If your local 'production-data/*' folders are EMPTY, it means the"
echo "   'mount' between your computer and Minikube is broken or not active."
echo
echo "--- How to Fix ---"
echo "The most reliable way to restore the mount is to restart Minikube with the mount flag."
echo "Run the following commands:"
echo
echo "  minikube stop"
echo "  ./run-k8s.sh"
echo
echo "If that doesn't work, you might need to delete and recreate the cluster (warning: deletes data):"
echo "  minikube delete"
echo "  ./run-k8s.sh"
