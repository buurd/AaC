# Strategy: Loyalty Program (Earn & Burn)

This document outlines the strategy for the Loyalty Program, defining the mechanics of earning and redeeming points, and describing the comprehensive user journey that will be verified by E2E tests.

## 1. The Core Concept: "Earn & Burn"

The Loyalty Program is designed to increase customer retention by rewarding purchases.
*   **Earn**: Customers receive points for every purchase.
*   **Burn**: Customers can redeem points for discounts on future orders.

## 2. The "Earn" Mechanics

### Trigger
Points are accrued when an Order is **Confirmed**. We do not award points for "Pending" orders to prevent fraud.

### Calculation Strategy
The `BonusRuleEngine` is responsible for calculating the points based on the order details.
*   **Base Rule**: 1 Euro spent = 1 Point earned.
*   **Multipliers**: The engine supports time-based rules (e.g., "Double Points in January").
*   **Item-Based Rules**: The engine supports product-based rules (e.g., "Buy 2 items, get 3x points"). *Note: This requires the Loyalty Service to receive the full list of order items, not just the total.*

### Data Flow
1.  **Order Service** confirms an order.
2.  **Order Service** sends an event/request to **Loyalty Service** with `OrderDTO` (Customer ID, Items, Total).
3.  **Loyalty Service** calculates points via `BonusRuleEngine`.
4.  **Loyalty Service** updates the customer's balance in `loyalty-db`.

## 3. The "Burn" Mechanics (Redemption)

### Trigger
Redemption happens during the **Checkout** process.

### Conversion Rate
*   10 Points = 1 Euro Discount.

### Data Flow
1.  **Webshop** fetches the user's balance from **Loyalty Service** to display in the UI.
2.  User selects "Apply Points" in the Cart.
3.  **Webshop** calculates the potential discount (Max discount = Order Total).
4.  **Order Service** receives the order with a `pointsRedeemed` field.
5.  **Order Service** calls **Loyalty Service** to `redeem` (deduct) the points.
    *   *Transactionality*: If the order fails, the points must be refunded (Compensating Transaction).

## 4. Comprehensive User Journey (E2E Test Scenario)

This narrative defines the "Happy Path" for the upcoming Cypress test implementation.

### Phase 1: The Baseline
1.  **Login**: Customer `user1` logs into the Webshop.
2.  **Check Balance**: `user1` views their profile.
    *   *Expectation*: Balance is **0 Points**.

### Phase 2: The "Earn" (First Purchase)
3.  **Browse**: `user1` navigates to products.
4.  **Add to Cart**: `user1` adds "Premium Headphones" (Price: **100 Euros**).
5.  **Checkout**: `user1` places the order.
6.  **Confirmation**: Order is successful.
7.  **Check Balance**: `user1` views their profile.
    *   *Expectation*: Balance is **100 Points** (100 Euros * 1x Base Rate).

### Phase 3: The "Burn" (Second Purchase)
8.  **Browse**: `user1` adds "USB Cable" (Price: **15 Euros**) to cart.
9.  **View Cart**: `user1` sees the option "Pay with Points".
    *   *UI*: "You have 100 Points (Value: 10 Euros)".
10. **Apply Points**: `user1` chooses to redeem 100 points.
11. **Price Update**: The Total Price updates from 15 Euros to **5 Euros**.
12. **Checkout**: `user1` places the order for 5 Euros.
13. **Confirmation**: Order is successful.

### Phase 4: The Verification
14. **Check Balance**: `user1` views their profile.
    *   *Expectation*: Balance is **5 Points**.
    *   *Logic*: Started with 100 -> Redeemed 100 (Balance 0) -> Paid 5 Euros -> Earned 5 Points.

## 5. Technical Implications

### New API Endpoints
*   `POST /api/loyalty/accrue`: Internal API for Order Service.
*   `POST /api/loyalty/redeem`: Internal API for Order Service.
*   `GET /api/loyalty/balance/{customerId}`: Public API (Secured) for Webshop.

### Data Consistency
*   For the MVP, we will use synchronous HTTP calls.
*   If `redeem` fails, the Order must fail.
*   If `accrue` fails, we log an error (Customer support can fix it manually). In the future, this should be an async message queue to ensure eventual consistency.

## 6. API Data Models

### A. Accrue Points Request
**Endpoint**: `POST /api/loyalty/accrue`
```json
{
  "customerId": "user-uuid-1234",
  "orderId": "order-5678",
  "totalAmount": 100.00,
  "currency": "EUR",
  "items": [
    {
      "productId": "prod-001",
      "quantity": 1,
      "price": 100.00,
      "category": "Electronics"
    }
  ]
}
```

### B. Redeem Points Request
**Endpoint**: `POST /api/loyalty/redeem`
```json
{
  "customerId": "user-uuid-1234",
  "orderId": "order-9012",
  "pointsToRedeem": 100
}
```
**Response**:
```json
{
  "status": "SUCCESS",
  "pointsRedeemed": 100,
  "remainingBalance": 0
}
```
*Error Response (409 Conflict)*: `{"status": "INSUFFICIENT_FUNDS"}`

### C. Get Balance Response
**Endpoint**: `GET /api/loyalty/balance/{customerId}`
```json
{
  "customerId": "user-uuid-1234",
  "points": 150,
  "value": 15.00,
  "currency": "EUR"
}
```

## 7. Loyalty Administration

### User Journey
A Loyalty Administrator needs a dashboard to:
*   View system-wide statistics (Total Points Issued).
*   View recent transactions (Customer, Points, Date).
*   Manage bonus rules (Enable/Disable).

### UI Requirements
The Loyalty Service will expose a simple HTML dashboard (Server-Side Rendered) at the root path (`/`).
*   The dashboard will display key metrics and provide controls to manage the system.
*   Authentication: The dashboard will be secured behind Keycloak, requiring the `loyalty-admin` role.

### Technical Implications
*   The `LoyaltyController` will be extended to serve the HTML dashboard.
*   A new `loyalty-admin` role will be defined in Keycloak.
*   A new endpoint will be added to fetch system-wide statistics (if needed).
