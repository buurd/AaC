describe('Navigation Links', () => {
  const webshopUrl = 'http://webshop-demo:8000';
  const pmUrl = 'http://pm-system:8001';
  const warehouseUrl = 'http://warehouse-demo:8002';

  it('should navigate correctly in Webshop', () => {
    cy.visit(webshopUrl);
    cy.contains('h1', 'Welcome to the Webshop!');
    cy.contains('View Products').click();
    cy.url().should('include', '/products');
    cy.contains('h1', 'Webshop Products');
  });

  it('should navigate correctly in Product Management', () => {
    cy.visit(pmUrl);
    cy.contains('h1', 'Product Management System');
    cy.contains('Manage Products').click();
    cy.url().should('include', '/products');
    cy.contains('h1', 'Product List');
  });

  it('should navigate correctly in Warehouse', () => {
    cy.visit(warehouseUrl);
    cy.contains('h1', 'Warehouse Service');
    
    // Check Products Link
    cy.contains('View Products').click();
    cy.url().should('include', '/products');
    cy.contains('h1', 'Warehouse Products');

    // Go back to root
    cy.visit(warehouseUrl);
    
    // Check Deliveries Link
    cy.contains('Manage Deliveries').click();
    cy.url().should('include', '/deliveries');
    cy.contains('h1', 'Deliveries');
  });
});