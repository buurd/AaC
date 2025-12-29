describe('Warehouse Order Fulfillment', () => {
  const warehouseUrl = 'https://reverse-proxy:8445';
  const productName = 'Fulfillment Test Product';

  it('should display confirmed orders in warehouse and allow fulfillment', () => {
    // 1. Setup: Create Product and Stock
    cy.clearCookies();
    cy.loginToPM();
    cy.createProductInPM({ name: productName, type: 'FulfillType', price: '25.00' });

    // Manual Sync required
    cy.contains('tr', productName).within(() => {
      cy.contains('Sync').click();
    });

    cy.clearCookies();
    cy.loginToWarehouse();
    cy.createDeliveryAndAddStock('Fulfill Supplier', productName, 'FT-1');

    // 2. Action: Place Order
    cy.clearCookies();
    cy.loginToWebshop();
    cy.addProductToCart(productName);
    cy.checkoutCart();
    
    // 3. Action: Confirm Order (Manual Step)
    cy.clearCookies();
    cy.loginToOrderService();
    cy.contains('tr', 'PENDING_CONFIRMATION', { timeout: 10000 }).within(() => {
        cy.contains('Confirm').click();
    });

    // 4. Verify: Order appears in Warehouse Fulfillment UI
    cy.clearCookies();
    cy.loginToWarehouse();
    cy.visit(warehouseUrl + '/fulfillment');
    cy.contains('h1', 'Order Fulfillment');
    
    // Wait for the order to appear (async process)
    cy.contains('td', 'PENDING', { timeout: 10000 }).should('be.visible');
    
    // 5. Action: Mark as Shipped
    cy.contains('tr', 'PENDING').within(() => {
        cy.contains('button', 'Mark Shipped').click();
    });
    
    // 6. Verify: Status Change
    cy.contains('td', 'SHIPPED').should('be.visible');
  });
});
