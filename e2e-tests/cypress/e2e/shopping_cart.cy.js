describe('Shopping Cart', () => {
  const webshopUrl = 'https://reverse-proxy:8443';
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
    cy.contains('.product-card', productName).within(() => {
      // Select Red S
      cy.get('select').eq(0).select('Red');
      cy.get('select').eq(1).select('S');
      cy.contains('button', 'Add to Cart').click();
    });

    // 3. Go to cart page
    cy.contains('button', 'Cart').click();
    cy.url().should('include', '/cart');
    cy.contains('h1', 'Shopping Cart');

    // 4. Verify product is in cart
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
    
    // 2. Get stock from UI
    cy.contains('.product-card', productName).within(() => {
      cy.get('select').eq(0).select('Red');
      cy.get('select').eq(1).select('S');
      cy.get('div[id^="product-display"]').invoke('text').as('stockText');
    });

    cy.get('@stockText').then((text) => {
      const stock = parseInt(text.match(/Stock: (\d+)/)[1]);
      
      // 3. Add product to cart until stock is depleted
      for (let i = 0; i < stock; i++) {
        cy.contains('.product-card', productName).within(() => {
            cy.contains('button', 'Add to Cart').click();
        });
      }

      // 4. Verify cart quantity
      cy.contains('button', 'Cart').click();
      cy.contains('tr', variantName).within(() => {
        cy.contains('td', stock);
      });

      // 5. Go back and try to add one more
      cy.contains('button', 'Back to Products').click();
      cy.contains('.product-card', productName).within(() => {
        cy.contains('button', 'Add to Cart').click();
      });

      // 6. Verify cart quantity is still the same
      cy.contains('button', 'Cart').click();
      cy.contains('tr', variantName).within(() => {
        cy.contains('td', stock);
      });
    });
  });
});
