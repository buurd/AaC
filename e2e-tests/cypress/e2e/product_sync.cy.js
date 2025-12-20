describe('Product Synchronization', () => {
  const productName = 'Sync Test Product';
  const webshopUrl = 'https://reverse-proxy:8443/products';

  // Helper function to poll the Webshop until content appears
  const verifyWebshopContent = (content, attempts = 0) => {
    if (attempts > 20) {
      throw new Error(`Timed out waiting for content: '${content}' in Webshop`);
    }
    cy.wait(1000);
    cy.request({ url: webshopUrl, failOnStatusCode: false }).then((res) => {
      if (res.status === 200 && res.body.includes(content)) {
        return; // Success
      }
      cy.log(`Content '${content}' not found yet. Retrying... (${attempts + 1}/20)`);
      cy.log(`Response Body: ${res.body}`); // Log the body to see what we got
      verifyWebshopContent(content, attempts + 1);
    });
  };

  // Helper function to poll the Webshop until content disappears
  const verifyWebshopContentMissing = (content, attempts = 0) => {
    if (attempts > 20) {
      throw new Error(`Timed out waiting for content to disappear: '${content}' in Webshop`);
    }
    cy.wait(1000);
    cy.request({ url: webshopUrl, failOnStatusCode: false }).then((res) => {
      if (res.status === 200 && !res.body.includes(content)) {
        return; // Success
      }
      cy.log(`Content '${content}' still present. Retrying... (${attempts + 1}/20)`);
      verifyWebshopContentMissing(content, attempts + 1);
    });
  };

  it('should sync create, update, and delete to webshop', () => {
    // 1. Create Product in PM (BaseUrl is PM HTTPS)
    cy.visit('/products');
    cy.contains('Create New Product').click();
    
    cy.get('input[name="type"]').clear().type('SyncType');
    cy.get('input[name="name"]').type(productName);
    cy.get('input[name="price"]').clear().type('100.00');
    cy.get('input[name="unit"]').clear().type('pcs');
    cy.get('input[name="description"]').type('Sync Description');
    cy.get('button[type="submit"]').click();

    // Verify in PM
    cy.contains(productName).should('be.visible');

    // 2. Verify in Webshop
    verifyWebshopContent(productName);
    verifyWebshopContent('100.00');

    // 3. Update Product in PM
    cy.contains('tr', productName).within(() => {
      cy.contains('Edit').click();
    });
    cy.get('input[name="price"]').clear().type('200.00');
    cy.get('button[type="submit"]').click();

    // Verify in PM
    cy.contains(productName).should('be.visible');
    cy.contains('200.00').should('be.visible');

    // 4. Verify Update in Webshop
    verifyWebshopContent('200.00');

    // 5. Delete Product in PM
    cy.contains('tr', productName).within(() => {
      cy.contains('Delete').click();
    });
    cy.get('button[type="submit"]').click();

    // Verify in PM
    cy.contains(productName).should('not.exist');

    // 6. Verify Delete in Webshop
    verifyWebshopContentMissing(productName);
  });
});