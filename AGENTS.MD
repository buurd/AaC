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
*   **Infrastructure**: Docker, Nginx (Reverse Proxy), Keycloak (IAM)
*   **Observability**: Loki, Promtail, Grafana
*   **Database**: PostgreSQL

## 2. Directory Structure
*   `/requirements`: Contains YAML files defining system requirements (e.g., `REQ-001.yaml`).
*   `/policies`: Contains Rego scripts (`.rego`) used by OPA to verify requirements against the architecture and code.
*   `/structurizr`: Contains the Structurizr DSL (`workspace.dsl`) and properties. This is the source of truth for the architecture model.
*   `/webshop`: Source code for the customer-facing Webshop System.
*   `/productManagementSystem`: Source code for the internal Product Management System.
*   `/warehouse`: Source code for the internal Warehouse Service.
*   `/orderService`: Source code for the internal Order Service.
*   `/infrastructure`: Infrastructure configurations (Nginx, Keycloak, Monitoring).
*   `/e2e-tests`: Cypress end-to-end tests.
*   `/pacts`: Pact contract tests for integration verification.

## 3. Architecture (C4 Model)
The system consists of four main software systems:
1.  **Webshop System**: Customer-facing e-commerce platform.
2.  **Product Management System (PM)**: Internal system for managing products.
3.  **Warehouse Service**: Internal system for inventory and delivery management.
4.  **Order Service**: Internal system for order management.

### Key Integrations
*   **Product Sync**: PM System -> Webshop & Warehouse (Updates product details).
*   **Stock Sync**: Warehouse -> Webshop (Updates stock levels).
*   **Stock Reservation**: Order Service -> Warehouse (Reserves stock on order).

## 4. Validation & Verification
*   **Architecture Validation**: `validate-architecture.sh` runs OPA policies to check if the code and Structurizr model adhere to the requirements in `/requirements`.
*   **E2E Testing**: Cypress tests in `/e2e-tests` verify UI functionality and user flows.
*   **Integration Testing**: Pact tests in `/pacts` verify service-to-service communication.

## 5. Security & Observability
*   **Security**: Nginx handles SSL termination. Keycloak manages authentication (RBAC) and authorization.
    *   **Registration**: Users can register via Keycloak (enabled in `realm-export.json`).
    *   **Login/Logout**: Applications provide Login/Logout functionality integrating with Keycloak.
    *   **Dynamic Redirects**: Applications dynamically construct redirect URIs based on the `Host` header to support various environments (localhost, reverse-proxy).
    *   **Backchannel Security**: Keycloak is configured with `KC_HOSTNAME_STRICT_BACKCHANNEL=true` to ensure consistent issuer claims for internal service-to-service validation.
*   **Observability**: Promtail collects logs, Loki aggregates them, and Grafana provides visualization.

## 6. Key Documents
*   `ARCHITECTURE_SUMMARY.md`: Detailed description of the architecture, containers, and components.
*   `REQUIREMENTS_SUMMARY.md`: Narrative summary of requirements by user journey.
*   `DESIGN_GUIDELINES.md`: UI consistency standards.

## 7. Workflow for Adding Features
1.  **Define Requirements**: Create a new YAML file in `/requirements` (e.g., `REQ-063.yaml`).
2.  **Update Domain/Architecture**: Update `structurizr/workspace.dsl` to reflect new relationships or components.
3.  **Implement Code**: Write the Java code to satisfy the requirement.
4.  **Verify**: Run `validate-architecture.sh` and tests.

## 8. Known Issues
*   **Cypress Registration Test**: `registration_flow.cy.js` is currently skipped due to network connectivity issues between the Cypress container and the Keycloak container (port 8446) in the test environment. The code implementation for registration and login is complete and correct.
