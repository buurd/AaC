# Architecture as Code: Project Summary

This document summarizes the "Architecture as Code" approach we have implemented for our system landscape. We have established a pipeline that ensures our implementation (Java code) remains in sync with our architectural model (Structurizr) and requirements.

## 1. The Architecture

We are building a landscape consisting of five distinct software systems following the **C4 Model**.

### System Context (Level 1)
*   **Webshop System**: The customer-facing e-commerce platform.
*   **Product Management System**: The internal system for managing product information (e.g., price, name).
*   **Warehouse Service**: The internal system for managing physical inventory and deliveries.
*   **Order Service**: The internal system for managing customer orders.
*   **Loyalty Service**: The internal system for managing customer bonus points.
*   **Observability System**: The infrastructure system for log aggregation and monitoring.
*   **Customer**: A person who uses the Webshop.
*   **Product Manager**: A person who manages products via the PM System.
*   **Warehouse Staff**: A person who manages inventory via the Warehouse Service.
*   **Order Manager**: A person who manages orders via the Order Service.
*   **Loyalty Administrator**: A person who manages the loyalty program.
*   **Developer**: A person who monitors the system logs.

### Containers (Level 2)
The landscape is composed of independent containers, deployed to **Kubernetes**, to ensure loose coupling:

**1. Webshop System**
*   **Webshop WebServer**: A Java application (Java 21) handling customer traffic.
*   **Webshop Database**: A dedicated PostgreSQL database for the Webshop.

**2. Product Management System**
*   **PM WebServer**: A Java application (Java 21) handling internal management traffic.
*   **PM Database**: A dedicated PostgreSQL database for the PM System.

**3. Warehouse Service**
*   **Warehouse WebServer**: A Java application (Java 21) handling inventory traffic.
*   **Warehouse Database**: A dedicated PostgreSQL database for the Warehouse Service.

**4. Order Service**
*   **Order WebServer**: A Java application (Java 21) handling order management traffic.
*   **Order Database**: A dedicated PostgreSQL database for the Order Service.

**5. Loyalty Service**
*   **Loyalty WebServer**: A Java application (Java 21) handling loyalty logic.
*   **Loyalty Database**: A dedicated PostgreSQL database for the Loyalty Service.

**6. Infrastructure & Security**
*   **API Gateway / Reverse Proxy**: An **Nginx** Ingress Controller handling SSL termination and routing.
*   **Keycloak IAM**: A centralized Identity and Access Management server handling authentication and authorization.

**7. Observability**
*   **Loki**: Log aggregation system.
*   **Promtail**: Log collector agent (DaemonSet).
*   **Grafana**: Visualization dashboard.

### Inter-System Communication
*   **Product Sync**: The **Product Management System** sends product updates (name, price, etc.) to both the **Webshop** and the **Warehouse Service**.
*   **Stock Sync**: The **Warehouse Service** sends stock level updates to the **Webshop** when inventory changes.
*   **Stock Reservation**: The **Order Service** reserves stock in the **Warehouse Service** when an order is placed.
*   **Loyalty Integration**: The **Order Service** and **Webshop** communicate with the **Loyalty Service** to accrue and redeem points.

### Components (Level 3)
Inside the Web Server containers, we have defined the following components:

*   **Webshop WebServer**:
    1.  **ProductController**: Handles product listing and "Add to Cart" functionality.
    2.  **ShoppingCartController**: Manages the client-side shopping cart view.
    3.  **OrderHistoryController**: Handles customer order history.
    4.  **ProductSyncController**: Handles product updates from PM.
    5.  **StockSyncController**: Handles stock updates from Warehouse.
    6.  **ProductRepository**: Manages data access to the Webshop Database.

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

*   **Order WebServer**:
    1.  **OrderController**: Handles order placement and management.
    2.  **StockReservationService**: Handles stock reservation with Warehouse.
    3.  **OrderRepository**: Manages data access to the Order Database.
    4.  **LoyaltyIntegrationService**: Handles interaction with Loyalty Service.

*   **Loyalty WebServer**:
    1.  **LoyaltyController**: Handles API and Dashboard requests.
    2.  **PointService**: Manages point accrual and redemption.
    3.  **BonusRuleEngine**: Calculates bonus points based on rules.
    4.  **LoyaltyRepository**: Manages data access to the Loyalty Database.

### Code (Level 4)
All systems implement a shared domain concept:
*   **Product**: A domain entity representing a product (ID, Name, Price, Description, Type, Unit).

---

## 2. Requirements & Traceability

We define our requirements as code (YAML), allowing us to trace them from high-level goals down to specific code implementations. The system now covers over 55 requirements, including:
*   **Architectural**: Defining the structure of all systems, their containers, and components.
*   **Functional**: Specifying user-facing features (CRUD in PM, Delivery Management in Warehouse, **Shopping Cart in Webshop**, **Order Management**, **Loyalty Program**) and system integrations.
*   **Runtime**: Ensuring services are live and accessible.
*   **Security**: Mandating HTTPS, Reverse Proxy, and IAM (Keycloak).
*   **Observability**: Mandating log aggregation and monitoring (Loki, Promtail, Grafana).
*   **Deployment**: Mandating Kubernetes deployment configuration (`REQ-078`).
*   **Design**: Enforcing a consistent UI via `06-Design_Guidelines.md`.

---

## 3. Security Architecture

We have implemented a robust security architecture:

### A. SSL Termination
*   **Reverse Proxy**: An **Nginx** Ingress Controller acts as the single entry point for all external traffic.
*   **HTTPS**: All traffic from users to the Reverse Proxy is encrypted via HTTPS (using self-signed certs for dev).
*   **Internal Traffic**: Traffic between the Reverse Proxy and the application containers is unencrypted HTTP within the trusted Kubernetes network.

### B. Identity and Access Management (IAM)
*   **Keycloak**: Used as the centralized IdP.
*   **Authentication**: Users (Product Managers, Warehouse Staff, Order Managers, Loyalty Admins) authenticate against Keycloak via the Reverse Proxy.
*   **Authorization**: Services enforce **Role-Based Access Control (RBAC)** using JWT tokens.
    *   **PM System**: Requires `product-manager` role.
    *   **Warehouse**: Requires `warehouse-staff` role.
    *   **Order Service**: Requires `order-manager` role.
    *   **Webshop Ordering**: Requires `customer` role.
    *   **Loyalty Service**: Requires `loyalty-admin` role for dashboard.
*   **Service-to-Service Security**: Internal synchronization calls use **Client Credentials Flow** to obtain service account tokens, ensuring secure M2M communication.

---

## 4. Observability Architecture

We have implemented a centralized logging and monitoring stack:
*   **Log Aggregation**: **Promtail** collects logs from all Kubernetes pods (Webshop, PM, Warehouse, Order, Loyalty, Nginx, Postgres) and pushes them to **Loki**.
*   **Visualization**: **Grafana** queries Loki to display log volumes and details.
*   **Dashboards**: A custom dashboard provides insights into log volume per container and log level (INFO, ERROR, etc.), with filtering capabilities.
*   **Standardization**: All Java applications use **SLF4J** to produce structured logs that are parsed by Promtail for better analysis.

---

## 5. Automated Validations

We have built a `validate-architecture.sh` script that runs a series of automated checks to enforce our architecture.

### A. Static Analysis (OPA & Rego)
We use **Open Policy Agent (OPA)** to validate our architecture model and code against our requirements.
*   **Structure**: Validates that all systems exist with their respective containers.
*   **Relations**: Ensures the model reflects the logical and physical dependencies.
*   **Code**: Scans Java source code to ensure domain entities and repositories are implemented correctly.
*   **Security**: Scans test code to ensure no insecure `http://` URLs are used.
*   **Contract Verification**: Checks that all integration requirements have a corresponding Pact contract file.
*   **Kubernetes Deployment**: Validates that the Kubernetes manifests (`infrastructure/k8s`) contain all required Deployments and Services as defined in `REQ-078`.
*   **Code Coverage**: Validates that all applications meet the minimum code coverage threshold of 80% as defined in `REQ-097`.

### B. Runtime Verification
We spin up the entire landscape (5 Apps, 5 DBs, 1 Proxy, 1 Keycloak, Observability Stack) in Docker/Kubernetes to verify the system works as expected.
*   **Infrastructure**: Starts all databases, Keycloak, Nginx, and Observability tools.
*   **Deployment**: Starts application containers with security configuration.
*   **Functional Check**: Verifies HTTP endpoints and redirects (302 Found) for secured resources.
*   **E2E Testing**: Uses **Cypress** to verify complex user flows and cross-system synchronization, running tests against the secure HTTPS endpoints and performing full login flows.
*   **Contract Testing**: Uses **Pact** to verify that consumer expectations match provider implementations for all inter-system integrations.

---

## 6. Visualizing the Architecture

We use **Structurizr** to visualize our model. The DSL file (`workspace.dsl`) is the source of truth for these diagrams. We use **tags** to create different views for different audiences:

*   **System Landscape View**: Shows the logical business context (Users -> Systems), hiding infrastructure details.
*   **Infrastructure View**: Shows the physical routing (Users -> Reverse Proxy -> Containers).
*   **Security View**: Shows the IAM architecture (Users/Services -> Keycloak).
*   **Observability View**: Shows the logging architecture (Promtail -> Loki -> Grafana).
*   **Dynamic Views**: Visualizes sequence diagrams for key flows:
    *   **User Login Flow**: Interaction between User, App, and Keycloak.
    *   **M2M Sync Flow**: Interaction between PM Service, Keycloak, and Webshop.
    *   **Stock Update Flow**: Interaction between Warehouse, Keycloak, and Webshop.
    *   **Product Variation Flow**: Interaction between PM Controller, Service, and downstream systems for variant generation.

---

## Conclusion

We have successfully scaled our **"Architecture as Code"** approach to a secure, multi-system landscape with observability.
*   **Decoupling**: We explicitly modeled and implemented separate databases to ensure loose coupling.
*   **Consistency**: We introduced Design Guidelines to maintain UI consistency across systems.
*   **Security**: We implemented a production-like security stack with Reverse Proxy and Keycloak IAM.
*   **Observability**: We added a full logging stack to monitor the health and activity of all services.
*   **New Features**: Added Order Service, Stock Reservation flows, Product Variations, and Loyalty Program.
*   **Verification**: Our automated pipeline now validates the integrity of a distributed system, ensuring that the implementation never drifts from the architectural intent.
