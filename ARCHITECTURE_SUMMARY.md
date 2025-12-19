# Architecture as Code: Project Summary

This document summarizes the "Architecture as Code" approach we have implemented for our system landscape. We have established a pipeline that ensures our implementation (Java code) remains in sync with our architectural model (Structurizr) and requirements.

## 1. The Architecture

We are building a landscape consisting of two distinct software systems following the **C4 Model**.

### System Context (Level 1)
*   **Webshop System**: The customer-facing e-commerce platform.
*   **Product Management System**: The internal system for managing product inventory.
*   **Customer**: A person who uses the Webshop.
*   **Product Manager**: A person who manages products via the PM System.

### Containers (Level 2)
The landscape is composed of independent containers to ensure loose coupling:

**1. Webshop System**
*   **Webshop WebServer**: A Java application (Java 21) handling customer traffic.
*   **Webshop Database**: A dedicated PostgreSQL database for the Webshop.

**2. Product Management System**
*   **PM WebServer**: A Java application (Java 21) handling internal management traffic.
*   **PM Database**: A dedicated PostgreSQL database for the PM System.

*(Note: The systems are currently decoupled at the data level, maintaining their own independent data stores.)*

### Components (Level 3)
Inside the Web Server containers, we have defined the following components:

*   **Webshop WebServer**:
    1.  **ProductController**: Handles the `/products` API endpoint.
    2.  **ProductRepository**: Manages data access to the Webshop Database.

*   **PM WebServer**:
    1.  **ProductController**: Handles CRUD operations (`/products/create`, `/edit`, `/delete`).
    2.  **ProductRepository**: Manages data access to the PM Database.

### Code (Level 4)
Both systems implement a shared domain concept:
*   **Product**: A domain entity representing a product (ID, Name, Price, Description, Type, Unit).

---

## 2. Requirements & Traceability

We define our requirements as code (YAML), allowing us to trace them from high-level goals down to specific code implementations.

| ID | Level | Description | Traceability |
| :--- | :--- | :--- | :--- |
| **REQ-001** | Context | Customer uses the Webshop system. | N/A |
| **REQ-002** | Context | Webshop is implemented as a Maven project. | N/A |
| **REQ-003** | Container | Webshop has a Web Server container. | Traces to REQ-001 |
| **REQ-004** | Container | Webshop has a Database container. | Traces to REQ-001 |
| **REQ-005** | Runtime | WebServer is accessible via HTTP (200 OK). | Traces to REQ-003 |
| **REQ-006** | Component | WebServer has a ProductController component. | Traces to REQ-003 |
| **REQ-007** | Runtime | `/products` endpoint returns a list of products. | Traces to REQ-006 |
| **REQ-008** | Code | `Product` domain entity exists with specific fields. | Traces to REQ-006 |
| **REQ-009** | Component | ProductController uses ProductRepository. | Traces to REQ-006 |
| **REQ-010** | Context | Product Manager uses the Product Management System. | N/A |
| **REQ-011** | Context | Product Management System is implemented as a Maven project. | Traces to REQ-010 |
| **REQ-012** | Container | Product Management System has a Web Server container. | Traces to REQ-010 |
| **REQ-013** | Container | Product Management System has a Database container. | Traces to REQ-010 |
| **REQ-014** | Runtime | PM WebServer is accessible via HTTP (200 OK). | Traces to REQ-012 |
| **REQ-015** | Runtime | PM System returns a list of products at `/products`. | Traces to REQ-014 |
| **REQ-016** | Runtime | PM System allows creating products UI at `/products/create`. | Traces to REQ-014 |
| **REQ-017** | Runtime | PM System allows editing products UI at `/products/edit`. | Traces to REQ-014 |
| **REQ-018** | Runtime | PM System allows deleting products UI at `/products/delete`. | Traces to REQ-014 |
| **REQ-019** | Code | PM System has a `Product` domain entity. | Traces to REQ-016 |
| **REQ-020** | Design | All UI must follow `DESIGN_GUIDELINES.md`. | Traces to REQ-001 |
| **REQ-021** | Functional | Create Product form persists data to DB. | Traces to REQ-016 |
| **REQ-022** | Functional | Edit Product form updates data in DB. | Traces to REQ-017 |
| **REQ-023** | Functional | Delete Product action removes data from DB. | Traces to REQ-018 |

---

## 3. Design Guidelines

To ensure a consistent user experience across the landscape, we have established a central design contract.

*   **Source of Truth**: `DESIGN_GUIDELINES.md`
*   **Enforcement**: `REQ-020` ensures that the implementation adheres to these guidelines.
*   **Scope**: Covers typography, color palette (Primary Blue, Success Green, Danger Red), and component styling (Tables, Buttons, Forms).

---

## 4. Automated Validations

We have built a `validate-architecture.sh` script that runs a series of automated checks to enforce our architecture.

### A. Static Analysis (OPA & Rego)
We use **Open Policy Agent (OPA)** to validate our architecture model and code against our requirements.
*   **Structure**: Validates that both systems (Webshop & PM) exist with their respective containers.
*   **Relations**: Ensures the model reflects that PM System uses its own Database and Webshop uses its own.
*   **Code**: Scans Java source code to ensure domain entities and repositories are implemented correctly.

### B. Runtime Verification
We spin up the entire landscape (2 Apps, 2 DBs) in Docker to verify the system works as expected.
*   **Infrastructure**: Starts `db-webshop` and `db-pm` PostgreSQL containers.
*   **Deployment**: Starts `webshop-demo` and `pm-demo` application containers.
*   **Functional Check**: Verifies HTTP endpoints, CRUD UI availability, and data persistence.

---

## 5. Visualizing the Architecture

We use **Structurizr** to visualize our model. The DSL file (`workspace.dsl`) is the source of truth for these diagrams.

*   **System Landscape View**: Shows the Customer, Product Manager, and both software systems.
*   **Container View**: Shows the internal containers (WebServer, Database) for each system.
*   **Component View**: Shows the internal components (Controller, Repository) for the WebServers.

---

## Conclusion

We have successfully scaled our **"Architecture as Code"** approach to a multi-system landscape.
*   **Decoupling**: We explicitly modeled and implemented separate databases to ensure loose coupling.
*   **Consistency**: We introduced Design Guidelines to maintain UI consistency across systems.
*   **Verification**: Our automated pipeline now validates the integrity of a distributed system, ensuring that the implementation never drifts from the architectural intent.
