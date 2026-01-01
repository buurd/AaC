# Database Schema Documentation

This document visualizes the database schemas for the four microservices in our landscape.

## 1. Webshop System (PostgreSQL)
The Webshop database stores product information synced from the PM system and local stock levels.

```mermaid
erDiagram
    products {
        int id PK
        int pm_id UK "Reference to PM System ID"
        string type
        string name
        text description
        numeric price
        string unit
        int stock "Synced from Warehouse"
    }
```

## 2. Product Management System (PostgreSQL)
The PM database is the source of truth for product information and handles product variations.

```mermaid
erDiagram
    product_groups {
        int id PK
        string name
        text description
        numeric base_price
        string base_unit
    }

    products {
        int id PK
        int group_id FK "Reference to product_groups"
        string type
        string name
        text description
        numeric price
        string unit
        text attributes "JSON: {Color: Red, Size: M}"
    }

    product_groups ||--|{ products : contains
```

## 3. Warehouse Service (PostgreSQL)
The Warehouse database manages physical inventory, deliveries, and fulfillment.

```mermaid
erDiagram
    products {
        int id PK
        int pm_id UK "Reference to PM System ID"
        string name
    }

    deliveries {
        int id PK
        string sender
    }

    product_individuals {
        int id PK
        int delivery_id FK
        int product_id FK
        string serial_number
        string state
    }

    fulfillment_orders {
        int id PK
        int order_id UK "Reference to Order Service ID"
        string status
    }

    deliveries ||--|{ product_individuals : contains
    products ||--|{ product_individuals : "is instance of"
```

## 4. Order Service (PostgreSQL)
The Order Service database manages customer orders and invoicing.

```mermaid
erDiagram
    orders {
        int id PK
        string customer_name
        string status
        timestamp created_at
    }

    order_items {
        int id PK
        int order_id FK
        int product_id "Reference to Webshop Product ID"
        int quantity
    }

    invoices {
        int id PK
        int order_id FK
        string customer_name
        decimal amount
        date due_date
        boolean paid
    }

    orders ||--|{ order_items : contains
    orders ||--|| invoices : generates
```
