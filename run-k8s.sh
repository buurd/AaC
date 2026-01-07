#!/bin/bash

clear

# Redirect all output to log file and stdout
mkdir -p logs
exec > >(tee -a logs/run-k8s.log) 2>&1

echo "--- Starting Kubernetes Setup ---"

# Define the project root and mount path
PROJECT_ROOT=$(pwd)
MOUNT_PATH="$PROJECT_ROOT"

# Check if Minikube is running
if minikube status | grep -q "Running"; then
    echo "Minikube is running."
    # Check if the mount is active (simple check if the process exists)
    if ! ps aux | grep -q "minikube mount"; then
        echo "⚠️  Minikube is running but might not have the mount active."
        echo "   Restarting mount in background..."
        # Start mount in background and log output
        nohup minikube mount "$PROJECT_ROOT:$MOUNT_PATH" > logs/minikube-mount.log 2>&1 &
        echo "   Mount restarted. Check logs/minikube-mount.log for details."
    fi
else
    echo "Minikube is not running. Starting Minikube with mount..."
    # Start Minikube with the mount
    # We mount the current directory to the same path in the VM to match the PV definitions
    minikube start --driver=docker --mount --mount-string="$PROJECT_ROOT:$MOUNT_PATH"

    # Enable Ingress addon
    echo "Enabling Ingress addon..."
    minikube addons enable ingress

    # Wait for kubectl to be ready
    echo "Waiting for cluster to be ready..."
    kubectl wait --for=condition=Ready nodes --all --timeout=60s
fi

# Ensure kubectl context is set to minikube
kubectl config use-context minikube

# Create a local Maven cache directory if it doesn't exist
mkdir -p ./m2-cache

# Function to build a service (Artifacts only)
build_artifacts() {
    SERVICE_NAME=$1
    SERVICE_DIR=$2

    echo "Building Artifacts for $SERVICE_NAME..."

    # Run Maven build using HOST Docker (not Minikube)
    # This ensures volume mounts work correctly
    docker run --rm \
        -v "$(pwd)/$SERVICE_DIR:/usr/src/mymaven" \
        -v "$(pwd)/m2-cache:/root/.m2" \
        -w /usr/src/mymaven \
        maven:3.9.6-eclipse-temurin-21 mvn clean package -DskipTests
}

# 1. Build JARs on Host
echo "--- Building Application Artifacts (Host) ---"
build_artifacts "Webshop" "applications/webshop"
build_artifacts "Product Management System" "applications/productManagementSystem"
build_artifacts "Warehouse Service" "applications/warehouse"
build_artifacts "Order Service" "applications/orderService"
build_artifacts "Loyalty Service" "applications/loyaltyService"

# 2. Switch to Minikube Docker Environment
echo "--- Switching to Minikube Docker Environment ---"
eval $(minikube docker-env)

# 3. Build Docker Images inside Minikube
# The build context (containing the JARs we just built) is sent to Minikube
echo "--- Building Docker Images in Minikube ---"

echo "Building Image: webshop"
docker build -t webshop:latest applications/webshop

echo "Building Image: product-management"
docker build -t product-management:latest applications/productManagementSystem

echo "Building Image: warehouse"
docker build -t warehouse:latest applications/warehouse

echo "Building Image: order-service"
docker build -t order-service:latest applications/orderService

echo "Building Image: loyalty-service"
docker build -t loyalty-service:latest applications/loyaltyService

# Create production data directories
mkdir -p "production-data/webshop-db"
mkdir -p "production-data/pm-db"
mkdir -p "production-data/warehouse-db"
mkdir -p "production-data/order-db"
mkdir -p "production-data/loyalty-db"
mkdir -p "production-data/loki"
mkdir -p "production-data/grafana"
mkdir -p "production-data/keycloak-db"

# Cleanup old resources to prevent conflicts
echo "--- Cleaning up old Kubernetes resources ---"
kubectl delete deployments --all -n infrastructure
kubectl delete daemonsets --all -n infrastructure
kubectl delete services --all -n infrastructure
kubectl delete ingresses --all -n infrastructure

# Apply Kubernetes configurations
echo "--- Applying Kubernetes configurations ---"
kubectl apply -f infrastructure/k8s/00-namespace.yaml
kubectl apply -f infrastructure/k8s/01-postgres-pv.yaml
kubectl apply -f infrastructure/k8s/07-infra-pv.yaml
kubectl apply -f infrastructure/k8s/02-secrets.yaml
kubectl apply -f infrastructure/k8s/03-databases.yaml
kubectl apply -f infrastructure/k8s/04-applications.yaml
kubectl apply -f infrastructure/k8s/05-ingress.yaml
kubectl apply -f infrastructure/k8s/06-observability.yaml
kubectl apply -f infrastructure/k8s/08-keycloak.yaml
kubectl apply -f infrastructure/k8s/09-loyalty-db.yaml
kubectl apply -f infrastructure/k8s/10-loyalty-app.yaml

# Force delete pods to ensure they are recreated with the new image
echo "Restarting application pods..."
# Delete all pods in the application namespaces to ensure a clean slate
kubectl delete pods --all -n webshop
kubectl delete pods --all -n product-management
kubectl delete pods --all -n warehouse
kubectl delete pods --all -n order-service
kubectl delete pods --all -n loyalty-service

echo "Kubernetes resources applied."
echo "Logs are being written to logs/run-k8s.log"

# Get Minikube IP
MINIKUBE_IP=$(minikube ip)
echo
echo "✅ Access your services at:"
echo "   Webshop:            http://$MINIKUBE_IP/webshop"
echo "   Product Management: http://$MINIKUBE_IP/pm"
echo "   Warehouse:          http://$MINIKUBE_IP/warehouse"
echo "   Order Service:      http://$MINIKUBE_IP/orders"
echo "   Loyalty Service:    http://$MINIKUBE_IP/loyalty (Internal)"
echo "   Grafana:            http://$MINIKUBE_IP/grafana"
echo "   Keycloak:           http://$MINIKUBE_IP/auth"
