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

Cypress.Commands.add('registerAndLoginToWebshop', (username, password) => {
  const webshopUrl = 'https://reverse-proxy:8443';
  cy.visit(webshopUrl);
  cy.contains('Register').click();
  // Keycloak Registration Flow
  // Note: Webshop redirects to Keycloak at https://reverse-proxy:8446
  cy.origin('https://reverse-proxy:8446', { args: { username, password } }, ({ username, password }) => {
      cy.get('#username').type(username);
      cy.get('#password').type(password);
      cy.get('#password-confirm').type(password);
      cy.get('#email').type(username + '@example.com');
      cy.get('#firstName').type('Test');
      cy.get('#lastName').type('User');
      cy.get('input[type="submit"]').click();
  });
  
  // Login
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

Cypress.Commands.add('loginToLoyaltyService', (username = 'l-user', password = 'l-user') => {
    const loyaltyUrl = 'https://reverse-proxy:8448';
    cy.visit(loyaltyUrl + '/'); // It redirects to login if not authenticated
    // Check if we are already logged in (url includes /) or redirected to login
    cy.url().then(url => {
        if (url.includes('/login')) {
            cy.get('input[name="username"]').type(username);
            cy.get('input[name="password"]').type(password);
            cy.get('button[type="submit"]').click();
        }
    });
    cy.url().should('include', loyaltyUrl + '/');
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
    cy.contains('button', 'View').click();
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
  // With the new UI, products are in cards, not rows (tr)
  // We look for the product name in a card
  // Increased timeout to 15000 to ensure file change is picked up and sync has time
  cy.contains('.product-card', productName, { timeout: 15000 }).within(() => {
    cy.contains('button', 'Add to Cart').click();
  });
  cy.get('@alertStub').should('have.been.calledWith', `Added ${productName} to cart!`);
});

Cypress.Commands.add('checkoutCart', () => {
  cy.contains('button', 'Cart').click();
  cy.contains('button', 'Checkout').click();
});
