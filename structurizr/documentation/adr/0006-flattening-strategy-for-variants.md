# 6. Flattening Strategy for Product Variations

Date: 2024-08-10

## Status
Accepted

## Context
We need to introduce "Product Variations" (e.g., Size, Color) into the system. The Product Management system needs to manage these hierarchies. However, downstream systems (Webshop, Warehouse, Order Service) are currently built around a flat "Product" concept. Refactoring all downstream systems to understand hierarchies would be a massive effort ("Big Bang").

## Decision
We will implement a **Flattening Strategy**. The PM system will manage the hierarchy (Product Groups -> Variants) but will sync them to downstream systems as individual, flat products with descriptive names (e.g., "T-Shirt - Red L").

## Consequences
### Positive
*   **Speed to Market**: Allows us to release the feature in PM without blocking on downstream refactoring.
*   **Stability**: Minimizes risk of breaking changes in the Webshop and Warehouse.

### Negative
*   **Technical Debt**: Downstream systems eventually need to understand grouping for a better UX (e.g., dropdowns on the product page). This is deferred work.
*   **Data Redundancy**: Similar products are stored as completely separate rows in downstream databases.
