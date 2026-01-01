describe('Shopping Cart', () => {
  const webshopUrl = 'https://reverse-proxy:8443';
  // Updated product name to match the new seed data (Variant 1)
  const productName = "Classic T-Shirt"; 
  const variantName = "Classic T-Shirt - Red S";

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
    // The UI now groups variants under the base name.
    // We need to find the card for "Classic T-Shirt" and click "Add to Cart".
    // The default selection is usually the first variant.
    cy.contains('.product-card', productName).within(() => {
      cy.contains('button', 'Add to Cart').click();
    });

    // 3. Go to cart page
    cy.contains('button', 'Cart').click();
    cy.url().should('include', '/cart');
    cy.contains('h1', 'Shopping Cart');

    // 4. Verify product is in cart
    // The cart should display the full variant name
    cy.contains('td', variantName).should('be.visible');
    cy.contains('td', '1').should('be.visible'); // Quantity

    // 5. Remove product from cart
    cy.contains('tr', variantName).within(() => {
      cy.contains('button', 'Remove').click();
    });

    // 6. Verify cart is empty
    cy.contains('td', 'Your cart is empty.').should('be.visible');
  });

  it('should not allow adding more items than available in stock', () => {
    // 1. Visit products page
    cy.visit(webshopUrl + '/products');
    
    // 2. Add product to cart until stock is depleted
    // The sample product (Red S) has stock=5
    for (let i = 0; i < 5; i++) {
      cy.contains('.product-card', productName).within(() => {
        cy.contains('button', 'Add to Cart').click();
      });
    }

    // 3. Verify cart quantity
    cy.contains('button', 'Cart').click();
    cy.contains('tr', variantName).within(() => {
      cy.contains('td', '5'); // Quantity should be 5
    });

    // 4. Go back and try to add one more
    cy.contains('button', 'Back to Products').click();
    cy.contains('.product-card', productName).within(() => {
      cy.contains('button', 'Add to Cart').click();
    });
    // We can't easily test the alert, but we can check the cart quantity hasn't changed

    // 5. Verify cart quantity is still 5
    cy.contains('button', 'Cart').click();
    cy.contains('tr', variantName).within(() => {
      cy.contains('td', '5');
    });
  });
});
