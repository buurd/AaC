describe('User Registration Flow', () => {
  const webshopUrl = 'https://reverse-proxy:8443';
  const keycloakUrl = 'https://reverse-proxy:8446';

  it('should allow a new user to register, login, and logout', () => {
    const username = `newuser_${Date.now()}`;
    const email = `${username}@example.com`;
    const password = 'password123';

    // 1. Construct the correct Registration URL manually
    const registerUrl = `${keycloakUrl}/realms/webshop-realm/protocol/openid-connect/registrations?client_id=webshop-client&response_type=code&scope=openid&redirect_uri=${webshopUrl}/`;

    // 2. Visit Keycloak Registration directly
    cy.origin(keycloakUrl, { args: { username, email, password, registerUrl } }, ({ username, email, password, registerUrl }) => {
      cy.visit(registerUrl);
      
      // Wait for form
      cy.get('input[name="username"]', { timeout: 10000 }).should('be.visible');
      
      cy.get('input[name="firstName"]').type('New');
      cy.get('input[name="lastName"]').type('User');
      cy.get('input[name="email"]').type(email);
      cy.get('input[name="username"]').type(username);
      cy.get('input[name="password"]').type(password);
      cy.get('input[name="password-confirm"]').type(password);
      cy.get('input[type="submit"]').click();
    });

    // 3. Should be redirected back to Webshop
    cy.url().should('include', webshopUrl);
    
    // 4. Login with the new credentials
    cy.contains('a', 'Login').click();
    
    cy.get('input[name="username"]').type(username);
    cy.get('input[name="password"]').type(password);
    cy.get('button[type="submit"]').click();
    
    // 5. Should be redirected to /products and have the cookie
    cy.url().should('include', '/products');
    cy.getCookie('webshop_auth_token').should('exist');

    // 6. Logout
    cy.contains('a', 'Logout').click();

    // 7. Handle Keycloak logout confirmation page
    cy.origin(keycloakUrl, () => {
      // Look for a button that confirms logout. Usually it's the primary button.
      // We try to find a button with text "Yes" or "Logout" or just the submit button.
      // Keycloak 23 default theme usually has a "Yes" button or "Logout".
      // Let's try to find the submit button in the form.
      cy.get('input[type="submit"]').click();
    });

    // 8. Should be redirected back to Webshop home and cookie should be gone
    cy.url().should('eq', webshopUrl + '/');
    cy.getCookie('webshop_auth_token').should('not.exist');
  });
});
