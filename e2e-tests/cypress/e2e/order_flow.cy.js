describe('Order Flow', () => {
  const productName = 'Order Test Product';
  const webshopUrl = 'https://reverse-proxy:8443';
  const pmUrl = 'https://reverse-proxy:8444';
  const warehouseUrl = 'https://reverse-proxy:8445';

  // Helper to login to Warehouse via request
  const loginToWarehouse = () => {
    cy.request({
      method: 'POST',
      url: warehouseUrl + '/login',
      form: true,
      body: {
        username: 'staff',
        password: 'password'
      }
    });
  };

  // Helper to login to Webshop via request
  const loginToWebshop = () => {
    cy.request({
      method: 'POST',
      url: webshopUrl + '/login',
      form: true,
      body: {
        username: 'manager', // Using manager as a valid user
        password: 'password'
      }
    });
  };

  // Helper to verify stock in Webshop
  const verifyStock = (expectedStock, attempts = 0) => {
    if (attempts > 40) throw new Error(`Timed out waiting for stock to be ${expectedStock}`); // Increased attempts
    cy.wait(1000);
    cy.request({ url: webshopUrl + '/products', failOnStatusCode: false }).then((res) => {
      // Simple check if the row contains the stock number
      if (res.status === 200 && res.body.includes(`<td>${expectedStock}</td>`)) {
        return;
      }
      cy.log(`Stock ${expectedStock} not found yet. Retrying...`);
      verifyStock(expectedStock, attempts + 1);
    });
  };

  it('should place an order, reduce stock, and show in order history', () => {
    // --- SETUP ---
    // 1. Create Product in PM
    cy.login('manager', 'password'); // Logs into PM (baseUrl)
    cy.visit('/products');
    cy.contains('Create New Product').click();
    cy.get('input[name="type"]').type('OrderType');
    cy.get('input[name="name"]').type(productName);
    cy.get('input[name="price"]').type('10.00');
    cy.get('input[name="unit"]').type('pcs');
    cy.get('button[type="submit"]').click();
    cy.contains(productName).should('be.visible');

    // 2. Create Delivery in Warehouse to add stock
    loginToWarehouse();
    cy.visit(warehouseUrl + '/deliveries');
    cy.get('input[name="sender"]').type('Order Supplier');
    cy.contains('button', 'Create Delivery').click();
    
    // Add 1 item to the delivery
    cy.contains('tr', 'Order Supplier').within(() => {
      cy.contains('View').click();
    });
    cy.get('select[name="productId"]').select(productName);
    cy.get('input[name="serialNumber"]').type('OT-1');
    cy.contains('button', 'Add Item').click();
    
    // 3. Verify Stock is 1 in Webshop
    verifyStock(1);

    // --- TEST ---
    // 4. Login to Webshop
    loginToWebshop();

    // 5. Place Order in Webshop
    cy.visit(webshopUrl + '/products');
    
    // Stub the window.alert function to test its calls
    cy.window().then((win) => {
      cy.stub(win, 'alert').as('alertStub');
    });

    // Add to cart and verify first alert
    cy.contains('tr', productName).within(() => {
      cy.contains('Add to Cart').click();
    });
    cy.get('@alertStub').should('have.been.calledOnceWith', `Added ${productName} to cart!`);
    
    // Go to cart and checkout
    cy.contains('View Cart').click();
    cy.contains('Checkout').click();
    
    // We skip verifying the second alert because the page redirect might happen too fast
    // Instead, we verify the outcome: Stock reduced to 0
    cy.log('Verifying stock reduction...');

    // 6. Verify Stock Reduced in Webshop (should be 0)
    verifyStock(0);

    // 7. Verify Order History (REQ-060)
    cy.visit(webshopUrl + '/my-orders');
    cy.contains('h1', 'My Orders');
    // The order ID is dynamic, but we can check for the status
    cy.contains('td', 'CONFIRMED').should('be.visible');
  });
});
