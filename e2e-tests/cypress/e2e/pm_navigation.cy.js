describe('Product Management Navigation', () => {
  const pmUrl = 'https://reverse-proxy:8444';

  beforeEach(() => {
    cy.loginToPM();
  });

  it('should have consistent navigation header on all pages', () => {
    // 1. Check Dashboard (Root)
    cy.visit(pmUrl);
    cy.contains('button', 'PM Dashboard').should('be.visible');
    cy.contains('button', 'Logout').should('be.visible');

    // 2. Check Product List
    cy.visit(pmUrl + '/products');
    cy.contains('button', 'PM Dashboard').should('be.visible').click();
    cy.url().should('eq', pmUrl + '/');
    
    // 3. Check Create Page
    cy.visit(pmUrl + '/products/create');
    cy.contains('button', 'PM Dashboard').should('be.visible');
    cy.contains('button', 'Logout').should('be.visible');
    
    // 4. Check Edit Page (need a product first)
    // Create a temp product
    cy.createProductInPM({ name: 'Nav Test Product', type: 'NavType', price: '10.00' });
    cy.contains('tr', 'Nav Test Product').within(() => {
      cy.contains('button', 'Edit').click();
    });
    cy.url().should('include', '/products/edit');
    cy.contains('button', 'PM Dashboard').should('be.visible');
    cy.contains('button', 'Logout').should('be.visible');

    // 5. Check Delete Page
    cy.visit(pmUrl + '/products');
    cy.contains('tr', 'Nav Test Product').within(() => {
      cy.contains('button', 'Delete').click();
    });
    cy.url().should('include', '/products/delete');
    cy.contains('button', 'PM Dashboard').should('be.visible');
    cy.contains('button', 'Logout').should('be.visible');

    // Cleanup
    cy.get('button[type="submit"]').click(); // Confirm delete
  });

  it('should logout correctly from header', () => {
    cy.visit(pmUrl + '/products');
    cy.contains('button', 'Logout').click();
    // Should be redirected to Keycloak logout and then back to app root (or login page depending on flow)
    // Since we are using a mock/test environment, we expect to land on a page where we are logged out.
    // The logout flow redirects to Keycloak, which redirects back to /.
    // We can check if the cookie is gone.
    cy.getCookie('auth_token').should('not.exist');
  });
});
