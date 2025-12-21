describe('Warehouse Synchronization', () => {
  const productName = 'Warehouse Sync Product';
  const productUpdatedName = 'Warehouse Sync Product Updated';
  const warehouseUrl = 'https://reverse-proxy:8445/products';
  const warehouseLoginUrl = 'https://reverse-proxy:8445/login';

  // Helper to login to Warehouse via request (to get cookie)
  const loginToWarehouse = () => {
    cy.request({
      method: 'POST',
      url: warehouseLoginUrl,
      form: true,
      body: {
        username: 'staff',
        password: 'password'
      }
    });
  };

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
    // Login to PM (UI)
    cy.login('manager', 'password');
    // Login to Warehouse (API) for verification
    loginToWarehouse();
  });

  it('should sync create, update, and delete to warehouse', () => {
    // 1. Create Product in PM (BaseUrl is PM HTTPS)
    cy.visit('/products');
    cy.contains('Create New Product').click();
    
    cy.get('input[name="type"]').clear().type('WarehouseType');
    cy.get('input[name="name"]').type(productName);
    cy.get('input[name="price"]').clear().type('50.00');
    cy.get('input[name="unit"]').clear().type('pcs');
    cy.get('input[name="description"]').type('Warehouse Description');
    cy.get('button[type="submit"]').click();

    // Verify in PM
    cy.contains(productName).should('be.visible');

    // 2. Verify in Warehouse
    verifyWarehouseContent(productName);

    // 3. Update Product in PM (Change Name)
    cy.contains('tr', productName).within(() => {
      cy.contains('Edit').click();
    });
    cy.get('input[name="name"]').clear().type(productUpdatedName);
    cy.get('button[type="submit"]').click();

    // Verify in PM
    cy.contains(productUpdatedName).should('be.visible');

    // 4. Verify Update in Warehouse
    verifyWarehouseContent(productUpdatedName);

    // 5. Delete Product in PM
    cy.contains('tr', productUpdatedName).within(() => {
      cy.contains('Delete').click();
    });
    cy.get('button[type="submit"]').click();

    // Verify in PM
    cy.contains(productUpdatedName).should('not.exist');

    // 6. Verify Delete in Warehouse
    verifyWarehouseContentMissing(productUpdatedName);
  });
});