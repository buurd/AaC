describe('Navigation Links', () => {
  const webshopUrl = 'https://reverse-proxy:8443';
  const pmUrl = 'https://reverse-proxy:8444';
  const warehouseUrl = 'https://reverse-proxy:8445';

  it('should navigate correctly in Webshop', () => {
    cy.visit(webshopUrl);
    cy.contains('h1', 'Welcome to the Webshop!');
    cy.contains('View Products').click();
    cy.url().should('include', '/products');
    cy.contains('h1', 'Webshop Products');
  });

  it('should navigate correctly in Product Management', () => {
    // Login first
    cy.visit(pmUrl + '/login');
    cy.get('input[name="username"]').type('manager');
    cy.get('input[name="password"]').type('password');
    cy.get('button[type="submit"]').click();

    cy.visit(pmUrl);
    cy.contains('h1', 'Product Management System');
    cy.contains('View Products').click();
    cy.url().should('include', '/products');
    cy.contains('h1', 'Products');
  });

  it('should navigate correctly in Warehouse', () => {
    // Login first
    cy.visit(warehouseUrl + '/login');
    cy.get('input[name="username"]').type('staff');
    cy.get('input[name="password"]').type('password');
    cy.get('button[type="submit"]').click();

    cy.visit(warehouseUrl);
    cy.contains('h1', 'Warehouse Service');
    
    // Check Products Link
    cy.contains('View Products').click();
    cy.url().should('include', '/products');
    cy.contains('h1', 'Warehouse Products');

    // Go back to root
    cy.visit(warehouseUrl);
    
    // Check Deliveries Link
    cy.contains('Deliveries').click();
    cy.url().should('include', '/deliveries');
    cy.contains('h1', 'Deliveries');
  });
});