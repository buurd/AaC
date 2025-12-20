# Architecture as Code: Project Summary

This document summarizes the "Architecture as Code" approach we have implemented for our system landscape. We have established a pipeline that ensures our implementation (Java code) remains in sync with our architectural model (Structurizr) and requirements.

## 1. The Architecture

We are building a landscape consisting of three distinct software systems following the **C4 Model**.

### System Context (Level 1)
*   **Webshop System**: The customer-facing e-commerce platform.
*   **Product Management System**: The internal system for managing product information (e.g., price, name).
*   **Warehouse Service**: The internal system for managing physical inventory and deliveries.
*   **Customer**: A person who uses the Webshop.
*   **Product Manager**: A person who manages products via the PM System.
*   **Warehouse Staff**: A person who manages inventory via the Warehouse Service.

### Containers (Level 2)
The landscape is composed of independent containers to ensure loose coupling:

**1. Webshop System**
*   **Webshop WebServer**: A Java application (Java 21) handling customer traffic.
*   **Webshop Database**: A dedicated PostgreSQL database for the Webshop.

**2. Product Management System**
*   **PM WebServer**: A Java application (Java 21) handling internal management traffic.
*   **PM Database**: A dedicated PostgreSQL database for the PM System.

**3. Warehouse Service**
*   **Warehouse WebServer**: A Java application (Java 21) handling inventory traffic.
*   **Warehouse Database**: A dedicated PostgreSQL database for the Warehouse Service.

### Inter-System Communication
*   **Product Sync**: The **Product Management System** sends product updates (name, price, etc.) to both the **Webshop** and the **Warehouse Service**.
*   **Stock Sync**: The **Warehouse Service** sends stock level updates to the **Webshop** when inventory changes.

---

## 2. Requirements & Traceability

We define our requirements as code (YAML), allowing us to trace them from high-level goals down to specific code implementations. The system now covers over 40 requirements, including:
*   **Architectural**: Defining the structure of all three systems, their containers, and components.
*   **Functional**: Specifying user-facing features (CRUD in PM, Delivery Management in Warehouse) and system integrations (Product and Stock Sync).
*   **Runtime**: Ensuring services are live and accessible.
*   **Security**: Mandating HTTPS and a Reverse Proxy.
*   **Design**: Enforcing a consistent UI via `DESIGN_GUIDELINES.md`.

---

## 3. Security Architecture

We have adopted an **SSL Termination** strategy to secure external access.

*   **Reverse Proxy**: An **Nginx** container acts as the single entry point for all external traffic.
*   **HTTPS**: All traffic from users to the Reverse Proxy is encrypted via HTTPS.
*   **Internal Traffic**: Traffic between the Reverse Proxy and the application containers, and between application containers, is currently unencrypted HTTP.

**Security Note**: This architecture relies on a "Security Bubble" assumption. The internal Docker network is considered a trusted zone. If an attacker gains access to this internal network, they can eavesdrop on inter-service communication. Future improvements may include implementing mTLS for zero-trust internal communication.

---

## 4. Automated Validations

We have built a `validate-architecture.sh` script that runs a series of automated checks to enforce our architecture.

### A. Static Analysis (OPA & Rego)
We use **Open Policy Agent (OPA)** to validate our architecture model and code against our requirements.
*   **Structure**: Validates that all systems (Webshop, PM, Warehouse) exist with their respective containers.
*   **Relations**: Ensures the model reflects the logical and physical dependencies.
*   **Code**: Scans Java source code to ensure domain entities and repositories are implemented correctly.

### B. Runtime Verification
We spin up the entire landscape (3 Apps, 3 DBs, 1 Proxy) in Docker to verify the system works as expected.
*   **Infrastructure**: Starts all databases and the Nginx Reverse Proxy.
*   **Deployment**: Starts `webshop-demo`, `pm-demo`, and `warehouse-demo` application containers.
*   **Functional Check**: Verifies HTTP endpoints, CRUD UI availability, and data persistence.
*   **E2E Testing**: Uses **Cypress** to verify complex user flows and cross-system synchronization, running tests against the secure HTTPS endpoints.

---

## 5. Visualizing the Architecture

We use **Structurizr** to visualize our model. The DSL file (`workspace.dsl`) is the source of truth for these diagrams. We use **tags** to create different views for different audiences:

*   **System Landscape View**: Shows the logical business context (Users -> Systems), hiding infrastructure details.
*   **Infrastructure View**: Shows the physical routing (Users -> Reverse Proxy -> Containers).
*   **Container & Component Views**: Show the internal structure of each service.

---

## Conclusion

We have successfully scaled our **"Architecture as Code"** approach to a multi-system landscape.
*   **Decoupling**: We explicitly modeled and implemented separate databases to ensure loose coupling.
*   **Consistency**: We introduced Design Guidelines to maintain UI consistency across systems.
*   **Security**: We added a Reverse Proxy for SSL termination, securing all external traffic.
*   **Verification**: Our automated pipeline now validates the integrity of a distributed system, ensuring that the implementation never drifts from the architectural intent.
