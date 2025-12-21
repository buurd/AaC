describe('Warehouse Delivery Management', () => {
  const warehouseUrl = 'https://reverse-proxy:8445';
  const senderName = 'Supplier A';
  const serialNumber = 'SN123';

  beforeEach(() => {
    // Login to Warehouse
    cy.visit(warehouseUrl + '/login');
    cy.get('input[name="username"]').type('staff');
    cy.get('input[name="password"]').type('password');
    cy.get('button[type="submit"]').click();
    cy.url().should('not.include', '/login');
  });

  it('should create delivery, add items, and return delivery', () => {
    // 1. Visit Warehouse (HTTPS)
    cy.visit(warehouseUrl);
    cy.contains('Manage Deliveries').click();
    
    // 2. Create Delivery
    cy.contains('h1', 'Deliveries');
    cy.get('input[name="sender"]').type(senderName);
    cy.contains('button', 'Create Delivery').click();

    // 3. Verify Delivery in List
    cy.contains('td', senderName).should('be.visible');

    // 4. View Delivery
    cy.contains('tr', senderName).within(() => {
      cy.contains('View').click();
    });
    cy.contains('h1', 'Delivery #');
    cy.contains('Sender: ' + senderName);

    // 5. Add Item
    // Assuming "Sample Product" exists (ID 1) from schema.sql
    cy.get('select[name="productId"]').select('Sample Product');
    cy.get('input[name="serialNumber"]').type(serialNumber);
    cy.get('select[name="state"]').select('New');
    // Ensure we click the button, not the h3 header
    cy.contains('button', 'Add Item').click();

    // 6. Verify Item
    cy.contains('td', serialNumber).should('be.visible');
    cy.contains('td', 'New').should('be.visible');

    // 7. Return to List
    cy.contains('Back to Deliveries').click();

    // 8. Return (Delete) Delivery
    cy.contains('tr', senderName).within(() => {
      cy.contains('Return').click();
    });

    // 9. Verify Delivery Gone
    cy.contains('td', senderName).should('not.exist');
  });
});