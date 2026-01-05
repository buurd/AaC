# AI Agent Instructions & Rules

This document outlines the strict protocols you must follow when assisting with the "Good Project".

## 1. The Golden Rule: Architecture as Code
This project enforces a strict synchronization between **Requirements**, **Architecture Model**, and **Code**.
*   **NEVER** propose code changes that contradict the architecture defined in `structurizr/workspace.dsl`.
*   **NEVER** implement a feature without a corresponding Requirement YAML file.
*   **ALWAYS** verify your changes using the validation scripts.

## 2. Context Loading
Before attempting to solve a problem, you must read the documentation in this folder to understand the context.
*   **Context**: `01-Project_Context.md`
*   **Architecture**: `02-Architecture_Overview.md`
*   **Requirements**: `03-Requirements.md`
*   **Validation Rules**: `04-Policies.md`
*   **Database**: `05-Database_Schema.md`
*   **UI/UX**: `06-Design_Guidelines.md`
*   **Strategies**: `07-Strategy_Product_Variations.md`

## 3. Development Workflow
When asked to add a feature or fix a bug, follow this process:

1.  **Requirements First**: Check `/requirements`. If the requirement is missing, draft a new YAML file (e.g., `REQ-099.yaml`) following the existing format.
2.  **Model Second**: If the change involves new containers, components, or relationships, update `/structurizr/workspace.dsl`.
3.  **Code Third**: Implement the changes in Java/Maven.
    *   Use **Java 21**.
    *   Follow **REST** principles.
    *   Ensure **Controller -> Service -> Repository** layering.
4.  **Verify**:
    *   **Static Analysis**: The system uses OPA (Rego) to validate structure.
    *   **Contract Tests**: Update Pact tests if API contracts change.
    *   **E2E Tests**: Update Cypress tests if UI flows change.

## 4. Validation
The project relies on `./validate-architecture.sh`. You should assume this script runs:
*   `check_traceability.rego`: Do requirements trace to parents?
*   `check_relation.rego`: Does the code match the C4 model?
*   `check_k8s_deployment.rego`: Do K8s manifests match `REQ-078`?

## 5. Technical Constraints
*   **Security**: All external traffic goes through Nginx (SSL). Internal traffic is HTTP.
*   **Auth**: Keycloak is the source of truth. Use RBAC.
*   **Logs**: All logs must be structured (SLF4J) for Promtail/Loki.
*   **Database**: PostgreSQL. Use the schema defined in `05-Database_Schema.md`.

## 6. Debugging
If the user reports issues, suggest using the provided scripts:
*   `./debug-k8s-status.sh` (General health)
*   `./debug-app-logs.sh` (Application logs)
*   `./debug-endpoints.sh` (Service connectivity)
