// --- Login Commands ---

Cypress.Commands.add('loginToPM', (username = 'manager', password = 'password') => {
  const pmUrl = 'https://reverse-proxy:8444';
  cy.visit(pmUrl + '/login');
  cy.get('input[name="username"]').type(username);
  cy.get('input[name="password"]').type(password);
  cy.get('button[type="submit"]').click();
  cy.url().should('include', '/products');
});

Cypress.Commands.add('loginToWarehouse', (username = 'staff', password = 'password') => {
  const warehouseUrl = 'https://reverse-proxy:8445';
  cy.visit(warehouseUrl + '/login');
  cy.get('input[name="username"]').type(username);
  cy.get('input[name="password"]').type(password);
  cy.get('button[type="submit"]').click();
  // Warehouse redirects to root /
  cy.url().should('eq', warehouseUrl + '/');
  cy.contains('h1', 'Warehouse Service');
});

Cypress.Commands.add('loginToWebshop', (username = 'manager', password = 'password') => {
  const webshopUrl = 'https://reverse-proxy:8443';
  cy.visit(webshopUrl + '/login');
  cy.get('input[name="username"]').type(username);
  cy.get('input[name="password"]').type(password);
  cy.get('button[type="submit"]').click();
  cy.url().should('include', '/products');
});

Cypress.Commands.add('loginToOrderService', (username = 'o-user', password = 'o-user') => {
  const orderUrl = 'https://reverse-proxy:8447';
  cy.visit(orderUrl + '/orders'); // It redirects to login if not authenticated
  // Check if we are already logged in (url includes /orders) or redirected to login
  cy.url().then(url => {
    if (url.includes('/login')) {
        cy.get('input[name="username"]').type(username);
        cy.get('input[name="password"]').type(password);
        cy.get('button[type="submit"]').click();
    }
  });
  // Order Service redirects to /orders
  cy.url().should('include', '/orders');
});

// --- Product Management Commands ---

Cypress.Commands.add('createProductInPM', (product) => {
  const pmUrl = 'https://reverse-proxy:8444';
  cy.visit(pmUrl + '/products');
  cy.contains('button', 'Create New Product').click();
  cy.get('input[name="type"]').clear().type(product.type || 'TestType');
  cy.get('input[name="name"]').type(product.name);
  cy.get('input[name="price"]').clear().type(product.price || '10.00');
  cy.get('input[name="unit"]').clear().type(product.unit || 'pcs');
  if (product.description) {
    cy.get('input[name="description"]').type(product.description);
  }
  cy.get('button[type="submit"]').click();
  cy.contains(product.name).should('be.visible');
});

Cypress.Commands.add('deleteProductInPM', (productName) => {
  const pmUrl = 'https://reverse-proxy:8444';
  cy.visit(pmUrl + '/products');
  cy.contains('tr', productName).within(() => {
    cy.contains('button', 'Delete').click();
  });
  cy.get('button[type="submit"]').click();
  cy.contains(productName).should('not.exist');
});

// --- Warehouse Commands ---

Cypress.Commands.add('createDeliveryAndAddStock', (senderName, productName, serialNumber = 'SN-1') => {
  const warehouseUrl = 'https://reverse-proxy:8445';
  cy.visit(warehouseUrl + '/deliveries');
  cy.get('input[name="sender"]').type(senderName);
  cy.contains('button', 'Create Delivery').click();
  
  cy.contains('tr', senderName).within(() => {
    cy.contains('View').click();
  });
  cy.get('select[name="productId"]').select(productName);
  cy.get('input[name="serialNumber"]').type(serialNumber);
  cy.contains('button', 'Add Item').click();
});

// --- Webshop Commands ---

Cypress.Commands.add('addProductToCart', (productName) => {
  const webshopUrl = 'https://reverse-proxy:8443';
  cy.visit(webshopUrl + '/products');
  // Stub alert to avoid blocking
  cy.window().then((win) => {
    cy.stub(win, 'alert').as('alertStub');
  });
  
  // Wait for product to appear (sync might take time)
  cy.contains('tr', productName, { timeout: 10000 }).within(() => {
    cy.contains('Add to Cart').click();
  });
  cy.get('@alertStub').should('have.been.calledWith', `Added ${productName} to cart!`);
});

Cypress.Commands.add('checkoutCart', () => {
  cy.contains('button', 'View Cart').click();
  cy.contains('Checkout').click();
});
