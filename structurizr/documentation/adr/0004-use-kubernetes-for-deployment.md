# 4. Use Kubernetes for Deployment

Date: 2024-05-15

## Status
Accepted

## Context
We are building a microservices-based landscape and need a robust, scalable, and standardized way to deploy and manage our services (Webshop, Product Management, Warehouse, Order Service) and their backing infrastructure (Databases, Keycloak, Observability).

The alternative is using a collection of disparate `docker-compose` files, which becomes difficult to manage and does not scale or provide production-grade features like self-healing and service discovery.

## Decision
We will use **Kubernetes** as our container orchestration platform. For local development, we will use **Minikube** to provide a lightweight, production-like environment.

All services will be defined as Kubernetes Deployments, Services, and Ingresses, with their configurations stored as code in the `infrastructure/k8s/` directory.

## Consequences
### Positive:
*   **Production-like Environment**: Our local development setup will closely mirror a real production environment.
*   **Standardization**: Provides a single, declarative way to define our entire system.
*   **Scalability & Resilience**: Kubernetes provides features like self-healing, rolling updates, and scaling out of the box.
*   **Infrastructure as Code**: The Kubernetes manifests are version-controlled alongside the application code.

### Negative:
*   **Increased Complexity**: Kubernetes has a steeper learning curve than Docker Compose.
*   **Resource Overhead**: Minikube and the Kubernetes control plane consume more system resources than a simple Docker setup.
*   **Configuration Management**: Requires careful management of Persistent Volumes, PVCs, and configuration, as we discovered during our debugging sessions.
