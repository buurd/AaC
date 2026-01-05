# Strategy: Product Variations (The Flattening Approach)

This document outlines the strategy for implementing Product Variations (Sizes, Colors, Weights) within the **Product Management System (PM)** while isolating downstream systems (Webshop, Warehouse, Order Service) from architectural changes in the short term.

## 1. The Core Concept: "Flattening"

To avoid a "big bang" refactor across all microservices, we will implement a **Flattening Strategy**.

*   **Internal Reality (PM System)**: Data is hierarchical. A `BaseProduct` (Parent) owns multiple `ProductVariants` (Children).
*   **External Reality (Downstream)**: Data remains flat. The downstream systems do not know about "Parents" or "Attributes". They only see individual, sellable items (SKUs).

## 2. Data Model Changes (PM System Only)

We will introduce a new entity in the PM Database, or extend the existing one, to support grouping.

### New Entity: `ProductGroup` (The Base Product)
*   **Fields**: `id`, `name`, `description`, `basePrice`, `baseWeight`.
*   **Role**: Acts as a template and a container. **This entity is NEVER synced directly to downstream systems.**

### Updated Entity: `Product` (The Variant)
*   **New Field**: `product_group_id` (Foreign Key).
*   **New Field**: `attributes` (JSON or Key-Value string, e.g., `{"Color": "Red", "Size": "L"}`).
*   **Role**: Represents the specific SKU. **This is what gets synced.**

## 3. Synchronization Logic (The Adapter Layer)

When a Product Manager clicks "Sync" on a **Product Group**, the PM System will perform the following logic:

1.  **Iterate**: Loop through all `Product` variants belonging to that group.
2.  **Construct Name**: Dynamically generate a display name for the downstream system.
    *   *Format*: `"{GroupName} - {AttributeValue1} {AttributeValue2}"`
    *   *Example*: "Classic T-Shirt - Red L"
3.  **Sync Individually**: Call the existing `POST /api/products/sync` endpoint on Webshop and Warehouse for *each* variant individually.

## 4. Phased Rollout

### Phase 1: PM Implementation (Current Focus)
*   Implement `ProductGroup` and Attribute management in PM.
*   Implement "Generate Variants" logic in PM.
*   Implement the "Flattening" sync logic.
*   **Result**: Warehouse and Webshop see distinct products ("Shirt Red S", "Shirt Red M") but treat them as completely unrelated items.

### Phase 2: Webshop Awareness (Future)
*   Update Webshop API to accept `groupId` and `attributes`.
*   Update Webshop UI to group items and show dropdowns.

### Phase 3: Warehouse Optimization (Future)
*   (Optional) Warehouse likely continues to prefer specific SKUs, so changes here might be minimal.

## 5. Impact on Requirements
*   **REQ-070**: Define Product Groups.
*   **REQ-071**: Define Attribute generation.
*   **REQ-072**: Define the Flattening Sync behavior.
