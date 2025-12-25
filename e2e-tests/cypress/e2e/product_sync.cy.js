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

  beforeEach(() => {
    cy.loginToPM();
  });

  it('should sync create, update, and delete to webshop', () => {
    // 1. Create
    cy.createProductInPM({ name: productName, type: 'SyncType', price: '100.00', description: 'Sync Description' });

    // 2. Verify Sync
    verifyWebshopContent(productName);
    verifyWebshopContent('100.00');

    // 3. Update
    cy.contains('tr', productName).within(() => {
      cy.contains('Edit').click();
    });
    cy.get('input[name="price"]').clear().type('200.00');
    cy.get('button[type="submit"]').click();
    cy.contains('200.00').should('be.visible');

    // 4. Verify Update Sync
    verifyWebshopContent('200.00');

    // 5. Delete
    cy.deleteProductInPM(productName);

    // 6. Verify Delete Sync
    verifyWebshopContentMissing(productName);
  });
});
