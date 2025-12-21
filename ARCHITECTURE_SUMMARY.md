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

**4. Infrastructure & Security**
*   **API Gateway / Reverse Proxy**: An **Nginx** container handling SSL termination and routing.
*   **Keycloak IAM**: A centralized Identity and Access Management server handling authentication and authorization.

### Inter-System Communication
*   **Product Sync**: The **Product Management System** sends product updates (name, price, etc.) to both the **Webshop** and the **Warehouse Service**.
*   **Stock Sync**: The **Warehouse Service** sends stock level updates to the **Webshop** when inventory changes.

### Components (Level 3)
Inside the Web Server containers, we have defined the following components:

*   **Webshop WebServer**:
    1.  **ProductController**: Handles product listing and "Add to Cart" functionality.
    2.  **ShoppingCartController**: Manages the client-side shopping cart view.
    3.  **ProductSyncController**: Handles product updates from PM.
    4.  **StockSyncController**: Handles stock updates from Warehouse.
    5.  **ProductRepository**: Manages data access to the Webshop Database.

*   **PM WebServer**:
    1.  **ProductController**: Handles CRUD operations.
    2.  **ProductService**: Orchestrates business logic and synchronization to Webshop and Warehouse.
    3.  **ProductRepository**: Manages data access to the PM Database.

*   **Warehouse WebServer**:
    1.  **ProductController**: Displays product list.
    2.  **DeliveryController**: Handles delivery management.
    3.  **ProductSyncController**: Handles product updates from PM.
    4.  **StockService**: Sends stock updates to Webshop.
    5.  **Repositories**: `ProductRepository` and `DeliveryRepository`.

### Code (Level 4)
All systems implement a shared domain concept:
*   **Product**: A domain entity representing a product (ID, Name, Price, Description, Type, Unit).

---

## 2. Requirements & Traceability

We define our requirements as code (YAML), allowing us to trace them from high-level goals down to specific code implementations. The system now covers over 45 requirements, including:
*   **Architectural**: Defining the structure of all three systems, their containers, and components.
*   **Functional**: Specifying user-facing features (CRUD in PM, Delivery Management in Warehouse, **Shopping Cart in Webshop**) and system integrations (Product and Stock Sync).
*   **Runtime**: Ensuring services are live and accessible.
*   **Security**: Mandating HTTPS, Reverse Proxy, and IAM (Keycloak).
*   **Design**: Enforcing a consistent UI via `DESIGN_GUIDELINES.md`.

---

## 3. Security Architecture

We have implemented a robust security architecture:

### A. SSL Termination
*   **Reverse Proxy**: An **Nginx** container acts as the single entry point for all external traffic.
*   **HTTPS**: All traffic from users to the Reverse Proxy is encrypted via HTTPS (using self-signed certs for dev).
*   **Internal Traffic**: Traffic between the Reverse Proxy and the application containers is unencrypted HTTP within the trusted Docker network.

### B. Identity and Access Management (IAM)
*   **Keycloak**: Used as the centralized IdP.
*   **Authentication**: Users (Product Managers, Warehouse Staff) authenticate against Keycloak via the Reverse Proxy.
*   **Authorization**: Services enforce **Role-Based Access Control (RBAC)** using JWT tokens.
    *   **PM System**: Requires `product-manager` role.
    *   **Warehouse**: Requires `warehouse-staff` role.
*   **Service-to-Service Security**: Internal synchronization calls use **Client Credentials Flow** to obtain service account tokens, ensuring secure M2M communication.

---

## 4. Automated Validations

We have built a `validate-architecture.sh` script that runs a series of automated checks to enforce our architecture.

### A. Static Analysis (OPA & Rego)
We use **Open Policy Agent (OPA)** to validate our architecture model and code against our requirements.
*   **Structure**: Validates that all systems exist with their respective containers.
*   **Relations**: Ensures the model reflects the logical and physical dependencies.
*   **Code**: Scans Java source code to ensure domain entities and repositories are implemented correctly.
*   **Security**: Scans test code to ensure no insecure `http://` URLs are used.

### B. Runtime Verification
We spin up the entire landscape (3 Apps, 3 DBs, 1 Proxy, 1 Keycloak) in Docker to verify the system works as expected.
*   **Infrastructure**: Starts all databases, Keycloak, and Nginx.
*   **Deployment**: Starts application containers with security configuration.
*   **Functional Check**: Verifies HTTP endpoints and redirects (302 Found) for secured resources.
*   **E2E Testing**: Uses **Cypress** to verify complex user flows and cross-system synchronization, running tests against the secure HTTPS endpoints and performing full login flows.

---

## 5. Visualizing the Architecture

We use **Structurizr** to visualize our model. The DSL file (`workspace.dsl`) is the source of truth for these diagrams. We use **tags** to create different views for different audiences:

*   **System Landscape View**: Shows the logical business context (Users -> Systems), hiding infrastructure details.
*   **Infrastructure View**: Shows the physical routing (Users -> Reverse Proxy -> Containers).
*   **Security View**: Shows the IAM architecture (Users/Services -> Keycloak).
*   **Dynamic Views**: Visualizes sequence diagrams for key flows:
    *   **User Login Flow**: Interaction between User, App, and Keycloak.
    *   **M2M Sync Flow**: Interaction between PM Service, Keycloak, and Webshop.
    *   **Stock Update Flow**: Interaction between Warehouse, Keycloak, and Webshop.

---

## Conclusion

We have successfully scaled our **"Architecture as Code"** approach to a secure, multi-system landscape.
*   **Decoupling**: We explicitly modeled and implemented separate databases to ensure loose coupling.
*   **Consistency**: We introduced Design Guidelines to maintain UI consistency across systems.
*   **Security**: We implemented a production-like security stack with Reverse Proxy and Keycloak IAM.
*   **New Features**: Added a client-side shopping cart to the Webshop, enhancing user interaction.
*   **Verification**: Our automated pipeline now validates the integrity of a distributed system, ensuring that the implementation never drifts from the architectural intent.
