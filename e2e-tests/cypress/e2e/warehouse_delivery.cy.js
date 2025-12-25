describe('Warehouse Delivery Management', () => {
  const warehouseUrl = 'https://reverse-proxy:8445';
  const senderName = 'Supplier A';
  const serialNumber = 'SN123';
  const productName = "The Hitchhiker's Guide to the Galaxy";

  beforeEach(() => {
    cy.loginToWarehouse();
  });

  it('should create delivery, add items, and return delivery', () => {
    // 1. Create Delivery
    cy.visit(warehouseUrl + '/deliveries');
    cy.get('input[name="sender"]').type(senderName);
    cy.contains('button', 'Create Delivery').click();
    cy.contains('td', senderName).should('be.visible');

    // 2. Add Item
    cy.contains('tr', senderName).within(() => {
      cy.contains('View').click();
    });
    cy.contains('h1', 'Delivery #');
    cy.get('select[name="productId"]').select(productName);
    cy.get('input[name="serialNumber"]').type(serialNumber);
    cy.get('select[name="state"]').select('New');
    cy.contains('button', 'Add Item').click();

    // 3. Verify Item
    cy.contains('td', serialNumber).should('be.visible');
    cy.contains('td', 'New').should('be.visible');

    // 4. Return Delivery
    cy.contains('Back to Deliveries').click();
    cy.contains('tr', senderName).within(() => {
      cy.contains('Return').click();
    });
    cy.contains('td', senderName).should('not.exist');
  });
});
