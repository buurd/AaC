describe('Shopping Cart', () => {
  const webshopUrl = 'https://reverse-proxy:8443';
  const productName = "The Hitchhiker's Guide to the Galaxy";

  beforeEach(() => {
    // Clear cart before each test
    cy.visit(webshopUrl + '/products');
    cy.window().then((win) => {
      win.localStorage.removeItem('cart');
    });
  });

  it('should add, view, and remove a product from the cart', () => {
    // 1. Visit products page
    cy.visit(webshopUrl + '/products');
    cy.contains('h1', 'Webshop Products');

    // 2. Add product to cart
    cy.contains('tr', productName).within(() => {
      cy.contains('button', 'Add to Cart').click();
    });

    // 3. Go to cart page
    cy.contains('button', 'Cart').click();
    cy.url().should('include', '/cart');
    cy.contains('h1', 'Shopping Cart');

    // 4. Verify product is in cart
    cy.contains('td', productName).should('be.visible');
    cy.contains('td', '1').should('be.visible'); // Quantity

    // 5. Remove product from cart
    cy.contains('tr', productName).within(() => {
      cy.contains('button', 'Remove').click();
    });

    // 6. Verify cart is empty
    cy.contains('td', 'Your cart is empty.').should('be.visible');
  });

  it('should not allow adding more items than available in stock', () => {
    // 1. Visit products page
    cy.visit(webshopUrl + '/products');
    
    // 2. Add product to cart until stock is depleted
    // The sample product has stock=10
    for (let i = 0; i < 10; i++) {
      cy.contains('tr', productName).within(() => {
        cy.contains('button', 'Add to Cart').click();
      });
    }

    // 3. Verify cart quantity
    cy.contains('button', 'Cart').click();
    cy.contains('tr', productName).within(() => {
      cy.contains('td', '10'); // Quantity should be 10
    });

    // 4. Go back and try to add one more
    cy.contains('button', 'Back to Products').click();
    cy.contains('tr', productName).within(() => {
      cy.contains('button', 'Add to Cart').click();
    });
    // We can't easily test the alert, but we can check the cart quantity hasn't changed

    // 5. Verify cart quantity is still 10
    cy.contains('button', 'Cart').click();
    cy.contains('tr', productName).within(() => {
      cy.contains('td', '10');
    });
  });
});
