# Good Project: An Architecture as Code Experiment

Welcome to **Good Project**. While this repository contains a functional microservices-based e-commerce landscape, its primary purpose is to serve as a proof-of-concept for **Architecture as Code**.

## 🎯 The Vision

In traditional software development, requirements, architectural diagrams, and code often drift apart. Documentation becomes stale, and architectural rules are forgotten.

This project implements a **"Double Bookkeeping"** approach to software engineering:
1.  **Requirements are Code**: Defined in structured YAML.
2.  **Architecture is Code**: Modeled in Structurizr DSL.
3.  **Infrastructure is Code**: Defined in Kubernetes manifests.
4.  **Validation is Automated**: We use Open Policy Agent (OPA) and Rego to ensure all the above remain in sync.

If the code deviates from the architecture, or if the deployment doesn't match the requirements, the build fails.

## 🛠️ The Validation Pipeline

The heart of this project is the `validate-architecture.sh` script. It treats the architecture as a testable artifact.

### 1. Static Analysis (OPA & Rego)
We use **Open Policy Agent (OPA)** to enforce strict rules:
*   **Traceability**: Every requirement must trace back to a parent requirement (e.g., `REQ-079` -> `REQ-078`).
*   **Model Consistency**: Relationships defined in requirements must exist in the Structurizr model.
*   **Implementation Check**: Components defined in the architecture must exist as Java classes.
*   **Kubernetes Validation**: The actual K8s manifests (`infrastructure/k8s`) are parsed and checked to ensure they contain the Deployments and Services mandated by the requirements.

### 2. Contract Testing (Pact)
We use **Pact** to verify inter-service communication. Instead of hoping services talk correctly, we define consumer-driven contracts that are verified against the provider's code.

### 3. Functional Verification (Cypress)
We spin up the entire ephemeral environment in Kubernetes and run **Cypress** E2E tests to validate the user journeys defined in the requirements.

## 🏗️ System Landscape

The system consists of four main microservices deployed on **Kubernetes (Minikube)**:

*   **Webshop**: Customer-facing storefront.
*   **Product Management**: Internal inventory and product catalog management.
*   **Warehouse**: Physical inventory and delivery tracking.
*   **Order Service**: Order processing and stock reservation.

Infrastructure includes **PostgreSQL** (one per service), **Keycloak** (IAM), **Nginx** (Ingress/Reverse Proxy), and a full Observability stack (**Loki, Promtail, Grafana**).

![System Landscape](documentation/diagrams/structurizr-SystemLandscape.png)

## 🚀 Runtime Modes

We support three distinct runtime modes depending on your needs:

### 1. Development Mode (Pure Docker)
Best for rapid development and testing. This mode spins up the services using standard Docker containers.
*   **Data**: Ephemeral (resets on restart).
*   **Seed Data**: Includes starter data for testing.
*   **Command**:
    ```bash
    ./run-webshop.sh
    ```

### 2. Production-like Mode (Kubernetes)
Best for verifying the deployment architecture and persistence. This runs the full landscape on Minikube.
*   **Data**: Persistent (stored in `production-data/` on host).
*   **Infrastructure**: Uses actual K8s manifests, Ingress controllers, and PVCs.
*   **Command**:
    ```bash
    ./run-k8s.sh
    ```
    *   **Webshop**: https://localhost:8443
    *   **Product Management**: https://localhost:8444
    *   **Warehouse**: https://localhost:8445
    *   **Keycloak**: https://localhost:8446
    *   **Order Service**: https://localhost:8447

### 3. Architecture Visualization
Runs the Structurizr Lite container to visualize the C4 model defined in `workspace.dsl`.
*   **Command**:
    ```bash
    ./run-structurizr.sh
    ```
*   **Access**: http://localhost:8080
*   **Export Images**: You can generate PNGs of all diagrams using:
    ```bash
    ./export-diagrams.sh
    ```

## ✅ Validating the Architecture
To run the full suite of OPA policies, contract tests, and E2E tests against the code and requirements:
```bash
./validate-architecture.sh
```

## 📚 Documentation

For deep dives into specific areas, please refer to the documentation folder:

*   **[Architecture Summary](documentation/ARCHITECTURE_SUMMARY.md)**: Detailed breakdown of containers, components, and security.
*   **[Requirements Summary](documentation/REQUIREMENTS_SUMMARY.md)**: Narrative view of all system requirements (User Journeys, Operations, etc.).
*   **[Validation Policies](documentation/POLICIES.md)**: Explanation of the Rego rules used to enforce the architecture.
*   **[Agent Context](documentation/AGENTS.MD)**: Technical summary for AI agents assisting with development.
*   **[Design Guidelines](documentation/DESIGN_GUIDELINES.md)**: UI/UX standards.

## 🔧 Debugging (K8s Mode)

If you encounter issues in the Kubernetes environment (e.g., 503 errors), use the provided debug scripts:
*   `./debug-k8s-status.sh`: Overview of pods and services.
*   `./debug-endpoints.sh`: Check service connectivity.
*   `./debug-app-logs.sh`: Tail application logs.

---
*This project was vibecoded with Gemini 2.0 Flash.*
