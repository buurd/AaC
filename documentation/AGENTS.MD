# Project Summary for Agents

This document serves as a persistent context for the AI agent to understand the "Good Project" structure, architecture, and requirements.

## 1. Project Overview
"Good Project" is a microservices-based e-commerce landscape implementing an "Architecture as Code" approach. It ensures synchronization between the architectural model (Structurizr), requirements (YAML), and implementation (Java code).

### Key Technologies
*   **Language**: Java 21 (Maven projects)
*   **Architecture Modeling**: Structurizr (C4 Model)
*   **Requirements Management**: YAML-based requirements
*   **Validation**: Open Policy Agent (OPA) & Rego scripts
*   **Testing**: Cypress (E2E), Pact (Contract Testing)
*   **Infrastructure**: Kubernetes (Minikube), Docker, Nginx (Reverse Proxy), Keycloak (IAM)
*   **Observability**: Loki, Promtail, Grafana
*   **Database**: PostgreSQL

## 2. Directory Structure
*   `/documentation`: Contains high-level documentation like summaries, strategies, and guidelines.
*   `/requirements`: Contains YAML files defining system requirements (e.g., `REQ-001.yaml`).
*   `/policies`: Contains Rego scripts (`.rego`) used by OPA to verify requirements against the architecture and code.
*   `/structurizr`: Contains the Structurizr DSL (`workspace.dsl`). This is the source of truth for the architecture model.
*   `/applications`: Contains the source code for the microservices.
*   `/infrastructure`: Contains infrastructure configurations (Kubernetes, Nginx, Keycloak, Monitoring).
*   `/e2e-tests`: Cypress end-to-end tests.
*   `/pacts`: Pact contract tests for integration verification.

## 3. Architecture (C4 Model)
The system consists of four main software systems:
1.  **Webshop System**: Customer-facing e-commerce platform.
2.  **Product Management System (PM)**: Internal system for managing products.
3.  **Warehouse Service**: Internal system for inventory and delivery management.
4.  **Order Service**: Internal system for order management.

### Key Integrations
*   **Product Sync**: PM System -> Webshop & Warehouse.
*   **Stock Sync**: Warehouse -> Webshop.
*   **Stock Reservation**: Order Service -> Warehouse.

## 4. Development Environment
The primary development environment is run on Kubernetes using **Minikube**.

*   **To start the entire environment**: Run `./run-k8s.sh`. This script handles Docker image building and Kubernetes deployment.
*   **To stop the environment**: Run `minikube stop`.

## 5. Validation & Verification
*   **Architecture Validation**: The `./validate-architecture.sh` script is the primary tool for ensuring the system is coherent. It runs multiple checks:
    *   **Static Analysis**: OPA policies in `/policies` are run against requirements, the Structurizr model, and source code. This now includes `check_k8s_deployment.rego` to validate the Kubernetes manifests against `REQ-078`.
    *   **Contract Testing**: Pact tests are executed to verify consumer-provider contracts.
    *   **Runtime & Functional Testing**: A temporary environment is spun up to run Cypress E2E tests.
*   **Integration Testing**: Pact tests in `/pacts` verify service-to-service communication.

## 6. Security & Observability
*   **Security**: Nginx handles SSL termination. Keycloak manages authentication (RBAC) and authorization.
*   **Observability**: Promtail collects logs, Loki aggregates them, and Grafana provides visualization.

## 7. Workflow for Adding Features
1.  **Define Requirements**: Create a new YAML file in `/requirements`. **Important**: Follow the `RequirementSet` format used by existing files (e.g., `REQ-079.yaml`) to ensure it's parsed correctly.
2.  **Update Domain/Architecture**: If necessary, update `structurizr/workspace.dsl`.
3.  **Implement Code**: Write the Java code, tests, and any new Rego policies.
4.  **Verify**: Run `./validate-architecture.sh` to ensure all checks pass.

## 8. Debugging & Common Issues
A suite of debugging scripts is available to inspect the running Kubernetes environment:
*   `./debug-k8s-status.sh`: Provides a comprehensive overview of all pods, services, and recent events. This is the best starting point for any issue.
*   `./debug-app-logs.sh`: Tails the logs for the main applications (`webshop`, `product-management`, `loki`).
*   `./debug-endpoints.sh`: Checks if the Kubernetes services are correctly pointing to running pods.
*   `./debug-storage.sh`: Inspects the contents of the persistent volumes.

### Common Problem: Pods are in `CrashLoopBackOff` or `Pending`
*   **Symptom**: Applications fail to start, often with database or volume-related errors. `debug-endpoints.sh` shows empty endpoints for the failing service.
*   **Cause**: This is typically due to an issue with Persistent Volume initialization, where the database container starts with a data directory that is not properly initialized.
*   **Solution**: The most reliable fix is to perform a clean reset of the environment:
    1.  Stop Minikube: `minikube stop`
    2.  Clean the persistent data directories: `sudo rm -rf production-data/*`
    3.  Restart the environment: `./run-k8s.sh`

## 9. Known Issues
*   The Kubernetes deployment is now stable after resolving database and Loki initialization issues. All validation checks are passing.
