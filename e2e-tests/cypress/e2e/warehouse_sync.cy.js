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
    // Since cy.request uses a separate cookie jar from cy.visit if not configured otherwise,
    // but here we use cy.request for verification.
    // Let's use the UI login helper which sets the cookie in the browser, 
    // but cy.request might not pick it up automatically unless we use cy.session or preserve cookies.
    // However, the previous test worked, so let's assume it shares the jar or we login via request.
    // Actually, the previous test used a helper `loginToWarehouse` which used `cy.request`.
    // Let's use the new `loginToWarehouse` command which uses `cy.visit`.
    // To make `cy.request` work, we might need to use `cy.request` for login too.
    
    // Let's just use the UI login for now, and if verification fails, we'll know.
    // Actually, `verifyWarehouseContent` uses `cy.request`.
    // If we login via UI, the cookie is in the browser. `cy.request` *does* attach cookies from the browser if they are set on the domain.
    // So `cy.loginToWarehouse()` (UI) should be enough if domains match.
    cy.loginToWarehouse();
  });

  it('should sync create, update, and delete to warehouse', () => {
    // 1. Create
    cy.createProductInPM({ name: productName, type: 'WarehouseType', price: '50.00', description: 'Warehouse Description' });

    // 2. Verify Sync
    verifyWarehouseContent(productName);

    // 3. Update
    // We need to switch back to PM to update.
    cy.loginToPM(); // Re-login or just visit if session persists. Session should persist.
    cy.visit('https://reverse-proxy:8444/products');
    
    cy.contains('tr', productName).within(() => {
      cy.contains('Edit').click();
    });
    cy.get('input[name="name"]').clear().type(productUpdatedName);
    cy.get('button[type="submit"]').click();
    cy.contains(productUpdatedName).should('be.visible');

    // 4. Verify Update Sync
    verifyWarehouseContent(productUpdatedName);

    // 5. Delete
    cy.loginToPM();
    cy.visit('https://reverse-proxy:8444/products');
    cy.deleteProductInPM(productUpdatedName);

    // 6. Verify Delete Sync
    verifyWarehouseContentMissing(productUpdatedName);
  });
});
