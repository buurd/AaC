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
*   `/documentation`: Contains high-level documentation like summaries, strategies, and guidelines.
*   `/requirements`: Contains YAML files defining system requirements (e.g., `REQ-001.yaml`).
*   `/policies`: Contains Rego scripts (`.rego`) used by OPA to verify requirements against the architecture and code.
*   `/structurizr`: Contains the Structurizr DSL (`workspace.dsl`) and properties. This is the source of truth for the architecture model.
*   `/applications/webshop`: Source code for the customer-facing Webshop System.
*   `/applications/productManagementSystem`: Source code for the internal Product Management System.
*   `/applications/warehouse`: Source code for the internal Warehouse Service.
*   `/applications/orderService`: Source code for the internal Order Service.
*   `/infrastructure`: Infrastructure configurations (Nginx, Keycloak, Monitoring).
*   `/e2e-tests`: Cypress end-to-end tests.
*   `/pacts`: Pact contract tests for integration verification.

## 3. Architecture (C4 Model)
The system consists of four main software systems:
1.  **Webshop System**: Customer-facing e-commerce platform.
2.  **Product Management System (PM)**: Internal system for managing products, including product groups and variants.
3.  **Warehouse Service**: Internal system for inventory and delivery management.
4.  **Order Service**: Internal system for order management.

### Key Integrations
*   **Product Sync**: PM System -> Webshop & Warehouse (Updates product details). Variants are "flattened" into individual products with descriptive names.
*   **Stock Sync**: Warehouse -> Webshop (Updates stock levels).
*   **Stock Reservation**: Order Service -> Warehouse (Reserves stock on order).

## 4. Validation & Verification
*   **Architecture Validation**: `validate-architecture.sh` runs OPA policies to check if the code and Structurizr model adhere to the requirements in `/requirements`.
*   **E2E Testing**: Cypress tests in `/e2e-tests` verify UI functionality and user flows.
*   **Integration Testing**: Pact tests in `/pacts` verify service-to-service communication. Requirements for integrations trace to `REQ-059` and are validated by the `check_pact_verification.rego` policy.

## 5. Security & Observability
*   **Security**: Nginx handles SSL termination. Keycloak manages authentication (RBAC) and authorization.
    *   **Registration**: Users can register via Keycloak (enabled in `realm-export.json`).
    *   **Login/Logout**: Applications provide Login/Logout functionality integrating with Keycloak.
    *   **Dynamic Redirects**: Applications dynamically construct redirect URIs based on the `Host` header to support various environments (localhost, reverse-proxy).
    *   **Backchannel Security**: Keycloak is configured with `KC_HOSTNAME_STRICT_BACKCHANNEL=true` to ensure consistent issuer claims for internal service-to-service validation.
*   **Observability**: Promtail collects logs, Loki aggregates them, and Grafana provides visualization.

## 6. Key Documents
*   `documentation/ARCHITECTURE_SUMMARY.md`: Detailed description of the architecture, containers, and components.
*   `documentation/REQUIREMENTS_SUMMARY.md`: Narrative summary of requirements by user journey.
*   `documentation/DESIGN_GUIDELINES.md`: UI consistency standards.
*   `documentation/STRATEGY_PRODUCT_VARIATIONS.md`: Strategy for implementing product variations (sizes, colors) using a flattening approach.
*   `documentation/POLICIES.md`: Describes the Rego policies used for validation.

## 7. Workflow for Adding Features
1.  **Define Requirements**: Create a new YAML file in `/requirements` (e.g., `REQ-076.yaml`). Ensure it traces to a parent requirement and any relevant policy requirements (e.g., `REQ-059` for integrations).
2.  **Update Domain/Architecture**: Update `structurizr/workspace.dsl` to reflect new relationships or components.
3.  **Implement Code**: Write the Java code to satisfy the requirement.
4.  **Verify**: Run `validate-architecture.sh` and tests.

## 8. Known Issues
*   The project is currently stable and all validation checks are passing.
