describe('Warehouse Synchronization', () => {
  const productName = 'Warehouse Sync Product';
  const productUpdatedName = 'Warehouse Sync Product Updated';
  const warehouseUrl = 'https://reverse-proxy:8445/products';

  // Helper function to poll the Warehouse until content appears
  const verifyWarehouseContent = (content, attempts = 0) => {
    if (attempts > 20) {
      throw new Error(`Timed out waiting for content: '${content}' in Warehouse`);
    }
    cy.wait(1000);
    // Ensure we are logged in (cookie is preserved)
    cy.request({ url: warehouseUrl, failOnStatusCode: false }).then((res) => {
      if (res.status === 200 && res.body.includes(content)) {
        return; // Success
      }
      cy.log(`Content '${content}' not found yet. Retrying... (${attempts + 1}/20)`);
      verifyWarehouseContent(content, attempts + 1);
    });
  };

  // Helper function to poll the Warehouse until content disappears
  const verifyWarehouseContentMissing = (content, attempts = 0) => {
    if (attempts > 20) {
      throw new Error(`Timed out waiting for content to disappear: '${content}' in Warehouse`);
    }
    cy.wait(1000);
    cy.request({ url: warehouseUrl, failOnStatusCode: false }).then((res) => {
      if (res.status === 200 && !res.body.includes(content)) {
        return; // Success
      }
      cy.log(`Content '${content}' still present. Retrying... (${attempts + 1}/20)`);
      verifyWarehouseContentMissing(content, attempts + 1);
    });
  };

  beforeEach(() => {
    cy.loginToPM();
    // We also need to be logged in to Warehouse to verify content via API
    // Clear cookies to avoid conflict if we switch users/apps
    cy.clearCookies();
    cy.loginToWarehouse();
  });

  it('should sync create, update, and delete to warehouse', () => {
    // 1. Create
    cy.clearCookies();
    cy.loginToPM();
    cy.createProductInPM({ name: productName, type: 'WarehouseType', price: '50.00', description: 'Warehouse Description' });

    // Manual Sync required
    cy.url().should('include', '/products');
    cy.contains('tr', productName).should('be.visible').within(() => {
      cy.contains('button', 'Sync').should('be.visible').click();
    });
    
    cy.wait(1000);

    // 2. Verify Sync
    // Need to login to Warehouse to verify
    cy.clearCookies();
    cy.loginToWarehouse();
    verifyWarehouseContent(productName);

    // 3. Update
    // We need to switch back to PM to update.
    cy.clearCookies();
    cy.loginToPM(); 
    cy.visit('https://reverse-proxy:8444/products');
    
    cy.contains('tr', productName).within(() => {
      cy.contains('button', 'Edit').click();
    });
    cy.get('input[name="name"]').clear().type(productUpdatedName);
    cy.get('button[type="submit"]').click();
    cy.contains(productUpdatedName).should('be.visible');

    // Manual Sync required
    cy.contains('tr', productUpdatedName).within(() => {
      cy.contains('button', 'Sync').should('be.visible').click();
    });
    
    cy.wait(1000);

    // 4. Verify Update Sync
    cy.clearCookies();
    cy.loginToWarehouse();
    verifyWarehouseContent(productUpdatedName);

    // 5. Delete
    cy.clearCookies();
    cy.loginToPM();
    cy.visit('https://reverse-proxy:8444/products');
    cy.deleteProductInPM(productUpdatedName);

    // 6. Verify Delete Sync
    cy.clearCookies();
    cy.loginToWarehouse();
    verifyWarehouseContentMissing(productUpdatedName);
  });
});
