describe('Order Service Navigation', () => {
  const orderUrl = 'https://reverse-proxy:8447';

  beforeEach(() => {
    cy.loginToOrderService();
  });

  it('should have consistent navigation header on all pages', () => {
    // 1. Check Dashboard (Root)
    cy.visit(orderUrl);
    cy.contains('button', 'Order Dashboard').should('be.visible');
    cy.contains('button', 'Logout').should('be.visible');

    // 2. Check Order List
    cy.visit(orderUrl + '/orders');
    cy.contains('button', 'Order Dashboard').should('be.visible').click();
    cy.url().should('eq', orderUrl + '/');
    
    // 3. Check Invoice List
    cy.visit(orderUrl + '/invoices');
    cy.contains('button', 'Order Dashboard').should('be.visible');
    cy.contains('button', 'Logout').should('be.visible');
  });

  it('should logout correctly from header', () => {
    cy.visit(orderUrl + '/orders');
    cy.contains('button', 'Logout').click();
    // Should be redirected to Keycloak logout and then back to app root (or login page depending on flow)
    cy.getCookie('auth_token').should('not.exist');
  });
});
