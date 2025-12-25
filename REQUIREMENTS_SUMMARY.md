# Requirements Summary

This document presents the system requirements in a narrative format, grouping them by user journey and architectural concern.

## 1. User Journeys

### The Webshop Experience (Customer)
The primary goal is to provide a seamless shopping experience for customers.
*   **Access**: The Customer uses the Webshop system (**REQ-001**).
*   **Registration**: Customers can register a new account (**REQ-063**).
*   **Navigation**: The root page provides clear navigation links to main functionalities (**REQ-040**).
*   **Browsing**: Customers can view a list of available products (**REQ-007**).
*   **Stock Visibility**: The current stock quantity is displayed for each product (**REQ-041**).
*   **Shopping Cart**: Customers can add products to a client-side shopping cart, view their cart, and remove items (**REQ-047**).
*   **Ordering**: Customers can place orders via the Order Service (**REQ-050**).
*   **Security**: Customers must be logged in to place an order (**REQ-064**).
*   **Order History**: Customers can view their past orders (**REQ-060**).

### Product Management (Product Manager)
Product Managers need tools to maintain the product catalog.
*   **Access**: The Product Manager uses the Product Management System (**REQ-010**).
*   **Product List**: Managers can view all products in the system (**REQ-015**).
*   **CRUD Operations**: The system provides a UI to Create (**REQ-016**, **REQ-021**), Edit (**REQ-017**, **REQ-022**), and Delete (**REQ-018**, **REQ-023**) products.

### Warehouse Operations (Warehouse Staff)
Warehouse Staff manage the physical inventory.
*   **Access**: The Warehouse Staff uses the Warehouse Service (**REQ-027**).
*   **Inventory View**: Staff can view the list of products known to the warehouse (**REQ-034**).
*   **Delivery Management**: The system supports the concept of Deliveries containing Product Individuals (**REQ-036**). Staff can create deliveries (**REQ-037**), add items to them (**REQ-038**), and return deliveries (**REQ-039**).
*   **Order Fulfillment**: Staff can view pending orders and mark them as shipped (**REQ-062**).

### Order Management (Order Manager)
Order Managers oversee the order fulfillment process.
*   **Access**: The Order Manager uses the Order Service (**REQ-051**).
*   **Order List**: Managers can view a list of placed orders (**REQ-052**).

---

## 2. System Integration

To ensure data consistency across the landscape, the systems synchronize data automatically.

### Product Synchronization (PM -> Webshop & Warehouse)
When a Product Manager changes the catalog, updates are propagated.
*   **Trigger**: The Product Management System sends updates when a product is created or updated (**REQ-025**) and deletions when a product is removed (**REQ-026**).
*   **Destinations**: Updates are sent to the Webshop (**REQ-024**) and the Warehouse Service (**REQ-033**, **REQ-035**).
*   **Mechanism**: The Warehouse exposes a sync API to receive these updates (**REQ-032**).

### Stock Synchronization (Warehouse -> Webshop)
When inventory changes in the warehouse, the webshop is updated.
*   **Trigger**: The Warehouse Service sends stock updates when inventory changes (e.g., delivery received) (**REQ-042**).
*   **Destination**: The Webshop receives these updates to display accurate stock to customers.

### Stock Reservation (Order -> Warehouse)
When an order is placed, stock must be reserved.
*   **Trigger**: The Order Service reserves stock when an order is created (**REQ-053**).
*   **Destination**: The Warehouse Service receives the reservation request (**REQ-054**).

### Order Fulfillment (Order -> Warehouse)
When an order is confirmed, the warehouse is notified.
*   **Trigger**: The Order Service notifies the Warehouse Service when an order is confirmed (**REQ-061**).
*   **Destination**: The Warehouse Service receives the notification to start fulfillment.

---

## 3. Security & Infrastructure

The system is built with security as a core concern.

*   **Secure Access**: All external access must be secured via HTTPS (**REQ-043**).
*   **Gateway**: A Reverse Proxy (Nginx) handles SSL termination and routing (**REQ-044**).
*   **Identity Management**: Keycloak is used as the centralized IAM provider (**REQ-045**).
*   **Authentication**: All services delegate authentication to Keycloak and verify JWT tokens for access (**REQ-046**).

---

## 4. Observability

The system includes comprehensive monitoring capabilities.

*   **System**: An Observability System is included for log aggregation and monitoring (**REQ-055**).
*   **Log Aggregation**: Loki is used to aggregate logs (**REQ-056**).
*   **Log Collection**: Promtail collects logs from all containers (**REQ-057**).
*   **Visualization**: Grafana is used for log visualization and dashboards (**REQ-058**).

---

## 5. Quality Assurance

*   **Contract Testing**: All inter-system integrations are verified using Pact contract tests (**REQ-059**).

---

## 6. Technical Architecture

The system follows a microservices-inspired architecture using the C4 model.

### Webshop System
*   **Implementation**: Maven project (**REQ-002**).
*   **Containers**: Web Server (**REQ-003**) and Database (**REQ-004**).
*   **Runtime**: Accessible via HTTP (**REQ-005**).
*   **Components**: `ProductController` (**REQ-006**) using `ProductRepository` (**REQ-009**).
*   **Domain**: `Product` entity (**REQ-008**).

### Product Management System
*   **Implementation**: Maven project (**REQ-011**).
*   **Containers**: Web Server (**REQ-012**) and Database (**REQ-013**).
*   **Runtime**: Accessible via HTTP (**REQ-014**).
*   **Domain**: `Product` entity (**REQ-019**).

### Warehouse Service
*   **Implementation**: Maven project (**REQ-028**).
*   **Containers**: Web Server (**REQ-029**) and Database (**REQ-030**).
*   **Runtime**: Accessible via HTTP (**REQ-031**).

### Order Service
*   **Implementation**: Maven project.
*   **Containers**: Web Server and Database.
*   **Runtime**: Accessible via HTTP.

---

## 7. Design Standards

*   **Consistency**: All user interfaces must adhere to the Design Guidelines defined in `DESIGN_GUIDELINES.md` (**REQ-020**).
