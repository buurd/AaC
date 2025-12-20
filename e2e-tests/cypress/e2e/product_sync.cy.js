describe('Product Synchronization', () => {
  const productName = 'Sync Test Product';
  const webshopUrl = 'http://webshop-demo:8000/products';

  it('should sync create, update, and delete to webshop', () => {
    // 1. Create Product in PM
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
    cy.wait(5000); 
    cy.request(webshopUrl).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body).to.include(productName);
      expect(response.body).to.include('100.00');
    });

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
    cy.wait(10000);
    cy.request(webshopUrl).then((response) => {
      if (!response.body.includes('200.00')) {
        throw new Error(`Expected 200.00 but got body: ${response.body}`);
      }
      expect(response.status).to.eq(200);
      expect(response.body).to.include(productName);
      expect(response.body).to.include('200.00');
    });

    // 5. Delete Product in PM
    cy.contains('tr', productName).within(() => {
      cy.contains('Delete').click();
    });
    cy.get('button[type="submit"]').click();

    // Verify in PM
    cy.contains(productName).should('not.exist');

    // 6. Verify Delete in Webshop
    cy.wait(5000);
    cy.request(webshopUrl).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body).to.not.include(productName);
    });
  });
});