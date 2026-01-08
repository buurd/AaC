describe('Warehouse Navigation', () => {
  const warehouseUrl = 'https://reverse-proxy:8445';

  beforeEach(() => {
    cy.loginToWarehouse();
  });

  it('should have consistent navigation header on all pages', () => {
    // 1. Check Dashboard (Root)
    cy.visit(warehouseUrl);
    cy.contains('button', 'Warehouse Dashboard').should('be.visible');
    cy.contains('button', 'Logout').should('be.visible');

    // 2. Check Product List
    cy.visit(warehouseUrl + '/products');
    cy.contains('button', 'Warehouse Dashboard').should('be.visible').click();
    cy.url().should('eq', warehouseUrl + '/');
    
    // 3. Check Deliveries List
    cy.visit(warehouseUrl + '/deliveries');
    cy.contains('button', 'Warehouse Dashboard').should('be.visible');
    cy.contains('button', 'Logout').should('be.visible');
    
    // 4. Check Delivery View (need to create one first)
    cy.get('input[name="sender"]').type('Nav Test Sender');
    cy.contains('button', 'Create Delivery').click();
    cy.contains('tr', 'Nav Test Sender').within(() => {
      cy.contains('button', 'View').click();
    });
    cy.url().should('include', '/deliveries/view');
    cy.contains('button', 'Warehouse Dashboard').should('be.visible');
    cy.contains('button', 'Logout').should('be.visible');

    // 5. Check Fulfillment Page
    cy.visit(warehouseUrl + '/fulfillment');
    cy.contains('button', 'Warehouse Dashboard').should('be.visible');
    cy.contains('button', 'Logout').should('be.visible');
  });

  it('should logout correctly from header', () => {
    cy.visit(warehouseUrl + '/products');
    cy.contains('button', 'Logout').click();
    // Should be redirected to Keycloak logout and then back to app root (or login page depending on flow)
    cy.getCookie('auth_token').should('not.exist');
  });
});
