# 5. Use Keycloak for Identity and Access Management

Date: 2024-06-15

## Status
Accepted

## Context
Our microservices landscape requires authentication and authorization for different user roles (Customer, Product Manager, Warehouse Staff). Building auth logic into each service leads to duplication and security risks. We need a centralized Identity Provider (IdP).

## Decision
We will use **Keycloak** as our centralized IAM solution. It will handle user management, authentication (OIDC), and authorization (RBAC).

## Consequences
### Positive
*   **Centralization**: Single point of control for all users and roles.
*   **Standardization**: Uses standard protocols (OpenID Connect, OAuth2).
*   **Security**: Offloads complex security logic (password hashing, token signing) to a dedicated, battle-tested product.

### Negative
*   **Resource Usage**: Keycloak is a heavy Java application, consuming significant memory in our local Minikube environment.
*   **Configuration**: Requires complex setup (Realms, Clients, Users) which we must automate or document carefully.
