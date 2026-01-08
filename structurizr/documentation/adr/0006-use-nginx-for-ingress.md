# 6. Use Nginx as Reverse Proxy/Ingress

Date: 2024-06-20

## Status
Accepted

## Context
We have multiple services (Webshop, PM, Warehouse, Keycloak) running in our cluster. We need a single entry point to route traffic to the correct service based on the URL path or hostname, and to handle SSL termination.

## Decision
We will use **Nginx** as our Ingress Controller / Reverse Proxy.

## Consequences
### Positive
*   **Single Entry Point**: Simplifies client configuration and DNS.
*   **SSL Termination**: Centralizes certificate management; backend services can speak plain HTTP internally.
*   **Routing Control**: Allows flexible routing rules (e.g., `/auth` -> Keycloak, `/` -> Webshop).

### Negative
*   **Configuration**: Requires maintaining Nginx configuration files or Ingress resources.
