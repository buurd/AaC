describe('Product Management CRUD', () => {
  const productName = 'Cypress Test Product';
  const productEditedName = 'Cypress Edited Product';

  beforeEach(() => {
    cy.login('manager', 'password');
  });

  it('should create, edit, and delete a product', () => {
    // 1. Visit the product list
    cy.visit('/products');
    cy.contains('h1', 'Product List');

    // 2. Create a new product
    cy.contains('Create New Product').click();
    cy.url().should('include', '/products/create');
    
    cy.get('input[name="type"]').clear().type('TestType');
    cy.get('input[name="name"]').type(productName);
    cy.get('input[name="price"]').clear().type('99.99');
    cy.get('input[name="unit"]').clear().type('unit');
    cy.get('input[name="description"]').type('Created by Cypress');
    
    cy.get('button[type="submit"]').click();

    // 3. Verify creation
    cy.url().should('include', '/products');
    cy.contains(productName).should('be.visible');

    // 4. Edit the product
    cy.contains('tr', productName).within(() => {
      cy.contains('Edit').click();
    });

    cy.url().should('include', '/products/edit');
    cy.get('input[name="name"]').clear().type(productEditedName);
    cy.get('button[type="submit"]').click();

    // 5. Verify edit
    cy.url().should('include', '/products');
    cy.contains(productName).should('not.exist');
    cy.contains(productEditedName).should('be.visible');

    // 6. Delete the product
    cy.contains('tr', productEditedName).within(() => {
      cy.contains('Delete').click();
    });

    cy.url().should('include', '/products/delete');
    cy.contains('Are you sure you want to delete');
    cy.get('button[type="submit"]').click();

    // 7. Verify deletion
    cy.url().should('include', '/products');
    cy.contains(productEditedName).should('not.exist');
  });
});