describe('Product Management CRUD', () => {
  const productName = 'Cypress Test Product';
  const productEditedName = 'Cypress Edited Product';

  beforeEach(() => {
    cy.loginToPM();
  });

  it('should create, edit, and delete a product', () => {
    // 1. Create
    cy.createProductInPM({ name: productName, type: 'TestType', price: '99.99', description: 'Created by Cypress' });

    // 2. Edit
    cy.contains('tr', productName).within(() => {
      cy.contains('button', 'Edit').click();
    });
    cy.url().should('include', '/products/edit');
    cy.get('input[name="name"]').clear().type(productEditedName);
    cy.get('button[type="submit"]').click();
    
    // Verify edit
    cy.url().should('include', '/products');
    cy.contains(productName).should('not.exist');
    cy.contains(productEditedName).should('be.visible');

    // 3. Delete
    cy.deleteProductInPM(productEditedName);
  });
});
