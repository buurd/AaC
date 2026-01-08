# 9. Introduce Loyalty Service

Date: 2024-08-25

## Status
Accepted

## Context
We want to introduce a loyalty program to incentivize customer purchases. The requirements involve complex, dynamic rules (e.g., "Double points in January", "Frequency bonuses") that are likely to change frequently.

Embedding this logic into the existing **Order Service** would violate the Single Responsibility Principle. The Order Service should focus on the transactional mechanics of taking an order and reserving stock. It should not be burdened with calculating marketing-driven bonus points.

## Decision
We will introduce a new, dedicated **Loyalty Service**.

*   **Responsibility**: It will manage customer point balances and evaluate bonus rules.
*   **Integration**:
    *   It will receive "Order Placed" events (or API calls) from the Order Service to calculate and accrue points.
    *   It will expose an API for the Webshop to display the customer's current balance.
*   **Data**: It will own its own database (`loyalty-db`) to store customer balances and transaction history.

## Consequences
### Positive
*   **Decoupling**: Marketing rules can change without redeploying the critical Order Service.
*   **Extensibility**: We can easily add new features like "Redeem Points" or "Tiered Membership" later.
*   **Performance**: Calculating complex rules won't slow down the checkout process (if implemented asynchronously in the future).

### Negative
*   **Operational Complexity**: Adds another container and database to manage in Kubernetes.
*   **Consistency**: There is a risk of data inconsistency if the Loyalty Service is down when an order is placed (requires eventual consistency mechanisms).
