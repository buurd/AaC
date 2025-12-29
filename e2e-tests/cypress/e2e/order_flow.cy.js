describe('Order Flow', () => {
  const productName = 'Order Test Product';
  const webshopUrl = 'https://reverse-proxy:8443';

  // Helper to verify stock in Webshop
  const verifyStock = (expectedStock, attempts = 0) => {
    if (attempts > 40) throw new Error(`Timed out waiting for stock to be ${expectedStock}`);
    cy.wait(1000);
    cy.request({ url: webshopUrl + '/products', failOnStatusCode: false }).then((res) => {
      if (res.status === 200 && res.body.includes(`<td>${expectedStock}</td>`)) {
        return;
      }
      cy.log(`Stock ${expectedStock} not found yet. Retrying...`);
      verifyStock(expectedStock, attempts + 1);
    });
  };

  it('should place an order, reduce stock, and show in order history', () => {
    // 1. Setup: Create Product and Stock
    cy.clearCookies();
    cy.loginToPM();
    cy.createProductInPM({ name: productName, type: 'OrderType' });

    // Manual Sync required
    cy.contains('tr', productName).within(() => {
      cy.contains('Sync').click();
    });

    cy.clearCookies();
    cy.loginToWarehouse();
    cy.createDeliveryAndAddStock('Order Supplier', productName, 'OT-1');
    
    // 2. Verify initial stock
    verifyStock(1);

    // 3. Action: Login and Place Order
    cy.clearCookies();
    cy.loginToWebshop();
    cy.addProductToCart(productName);
    cy.checkoutCart();
    
    // 4. Verify: Stock reduced
    cy.log('Verifying stock reduction...');
    verifyStock(0);

    // 5. Action: Confirm Order (Manual Step)
    cy.clearCookies();
    cy.loginToOrderService();
    // Wait for order to appear
    cy.contains('tr', 'PENDING_CONFIRMATION', { timeout: 10000 }).within(() => {
        cy.contains('Confirm').click();
    });
    // Verify status changed to CONFIRMED
    cy.contains('tr', 'CONFIRMED').should('be.visible');

    // 6. Verify: Order History in Webshop
    cy.clearCookies();
    cy.loginToWebshop();
    cy.visit(webshopUrl + '/my-orders');
    cy.contains('h1', 'My Orders');
    cy.contains('td', 'CONFIRMED').should('be.visible');
  });
});
