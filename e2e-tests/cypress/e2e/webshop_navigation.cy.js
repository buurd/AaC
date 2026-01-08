describe('Webshop Navigation', () => {
  const webshopUrl = 'https://reverse-proxy:8443';
  const username = 'nav_user_' + Date.now();
  const password = 'password123';

  before(() => {
      cy.registerAndLoginToWebshop(username, password);
  });

  it('should have consistent navigation header on all pages', () => {
    // Ensure logged in
    cy.loginToWebshop(username, password);

    // 1. Check Dashboard (Root)
    cy.visit(webshopUrl);
    cy.contains('button', 'Webshop Home').should('be.visible');
    cy.contains('button', 'Products').should('be.visible');
    cy.contains('button', 'Cart').should('be.visible');
    cy.contains('button', 'My Orders').should('be.visible');
    cy.contains('button', 'Logout').should('be.visible');

    // 2. Check Product List
    cy.contains('button', 'Products').click();
    cy.url().should('include', '/products');
    cy.contains('button', 'Webshop Home').should('be.visible');
    
    // 3. Check Cart
    cy.contains('button', 'Cart').click();
    cy.url().should('include', '/cart');
    cy.contains('button', 'Webshop Home').should('be.visible');

    // 4. Check My Orders
    cy.contains('button', 'My Orders').click();
    cy.url().should('include', '/my-orders');
    cy.contains('button', 'Webshop Home').should('be.visible');

    // 5. Check Home Link
    cy.contains('button', 'Webshop Home').click();
    cy.url().should('eq', webshopUrl + '/');
  });

  it('should logout correctly from header', () => {
    cy.loginToWebshop(username, password);
    cy.visit(webshopUrl + '/products');
    cy.contains('button', 'Logout').click();
    // Should be redirected to Keycloak logout and then back to app root (or login page depending on flow)
    cy.getCookie('webshop_auth_token').should('not.exist');
  });
});
