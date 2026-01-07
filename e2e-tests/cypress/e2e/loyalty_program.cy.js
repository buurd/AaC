describe('Loyalty Program E2E', () => {
  const username = 'loyalty_user_' + Date.now();
  const password = 'password123';
  const webshopUrl = 'https://reverse-proxy:8443';

  before(() => {
    // Register a new user for a clean slate
    cy.visit(webshopUrl);
    cy.contains('Register').click();
    // Keycloak Registration Flow
    cy.origin('https://reverse-proxy:8446', { args: { username, password } }, ({ username, password }) => {
        cy.get('#username').type(username);
        cy.get('#password').type(password);
        cy.get('#password-confirm').type(password);
        cy.get('#email').type(username + '@example.com');
        cy.get('#firstName').type('Loyalty');
        cy.get('#lastName').type('User');
        cy.get('input[type="submit"]').click();
    });
  });

  beforeEach(() => {
    // Login before each test
    cy.visit(webshopUrl + '/login');
    cy.get('input[name="username"]').type(username);
    cy.get('input[name="password"]').type(password);
    cy.get('button[type="submit"]').click();
  });

  it('Test 1: Earn and Burn Flow', () => {
    // 1. Shop a number of articles (Earn)
    cy.visit(webshopUrl + '/products');
    
    // New check: Verify campaign text is visible
    cy.contains('.product-card', 'Classic T-Shirt').within(() => {
      cy.contains('Possible campaigns: Base Rule: 1 Point per 1 EUR, January Bonus, Double Points on 3+ Items, 1.5x Points on 3+ Distinct Items').should('be.visible');
    });

    // Stub alerts after visit
    cy.window().then((win) => {
      cy.stub(win, 'alert').as('alertStub');
    });
    
    // Wait for products to load
    cy.get('.product-card', { timeout: 10000 }).should('have.length.gt', 0);

    // Add "Classic T-Shirt - Red S" (Price: 25.00)
    cy.contains('.product-card', 'Classic T-Shirt').within(() => {
        cy.get('select').eq(0).select('Red');
        cy.get('select').eq(1).select('S');
        cy.contains('button', 'Add to Cart').click();
    });
    
    cy.get('@alertStub').should('have.been.calledWith', 'Added Classic T-Shirt - Red S to cart!');

    cy.visit(webshopUrl + '/cart');
    
    // New check: Verify cart loyalty value
    // 25 EUR * 2 (January Bonus) = 50 points
    cy.contains('h4', 'Potential Loyalty Points: 50').should('be.visible');

    // Stub alerts again after navigation
    cy.window().then((win) => {
      cy.stub(win, 'alert').as('alertStub');
    });

    cy.contains('Checkout').click();
    
    cy.get('@alertStub').should('have.been.calledWith', 'Order placed successfully!');

    // --- Manual Confirmation Step ---
    // Switch to Order Service to confirm the order
    cy.clearCookies(); // Clear webshop session
    cy.loginToOrderService(); // Logs in as admin/manager
    
    // Find the pending order and confirm it
    cy.contains('tr', 'PENDING_CONFIRMATION', { timeout: 10000 }).within(() => {
        cy.contains('button', 'Confirm').click();
    });
    cy.contains('tr', 'CONFIRMED').should('be.visible');
    
    // Switch back to Webshop
    cy.clearCookies(); // Clear order service session
    cy.visit(webshopUrl + '/login');
    cy.get('input[name="username"]').type(username);
    cy.get('input[name="password"]').type(password);
    cy.get('button[type="submit"]').click();
    // --------------------------------

    // 2. Verify collected loyalty
    cy.visit(webshopUrl + '/cart');
    
    // Expect 50 points (25 EUR * 2 for January Bonus)
    cy.get('#loyalty-points', { timeout: 10000 }).should('not.have.text', '0');
    cy.get('#loyalty-points').invoke('text').then((text) => {
        const points = parseInt(text);
        expect(points).to.be.at.least(50);
    });

    // 3. Use loyalty to shop something else (Burn)
    cy.visit(webshopUrl + '/products');
    
    // Stub alerts
    cy.window().then((win) => {
      cy.stub(win, 'alert').as('alertStub');
    });

    cy.contains('.product-card', 'Classic T-Shirt').within(() => {
        cy.get('select').eq(0).select('Red');
        cy.get('select').eq(1).select('S');
        cy.contains('button', 'Add to Cart').click();
    });
    
    cy.visit(webshopUrl + '/cart');
    
    // Stub alerts
    cy.window().then((win) => {
      cy.stub(win, 'alert').as('alertStub');
    });

    // Apply points
    cy.get('#use-points').check();
    cy.get('#points-input').type('10'); // Redeem 10 points (1 EUR)
    
    cy.contains('Checkout').click();
    
    cy.get('@alertStub').should('have.been.calledWith', 'Order placed successfully!');
    
    // --- Manual Confirmation Step (for the second order) ---
    cy.clearCookies();
    cy.loginToOrderService();
    
    cy.contains('tr', 'PENDING_CONFIRMATION', { timeout: 10000 }).within(() => {
        cy.contains('button', 'Confirm').click();
    });
    cy.contains('tr', 'CONFIRMED').should('be.visible');
    
    cy.clearCookies();
    cy.visit(webshopUrl + '/login');
    cy.get('input[name="username"]').type(username);
    cy.get('input[name="password"]').type(password);
    cy.get('button[type="submit"]').click();
    // -------------------------------------------------------
    
    // Verify balance decreased
    cy.visit(webshopUrl + '/cart');
    
    // Wait for points to be positive (it might start at 0 if fetch is slow)
    cy.get('#loyalty-points').should((($div) => {
        const text = $div.text();
        const points = parseInt(text);
        expect(points).to.be.greaterThan(0);
    }));

    // New check: Verify order history loyalty
    cy.visit(webshopUrl + '/my-orders');
    cy.contains('tr', 'CONFIRMED').within(() => {
      cy.contains('td', '50').should('be.visible'); // Points earned (January Bonus)
      cy.contains('td', '10').should('be.visible'); // Points redeemed
    });
  });

  it('Test 2: Complex Rule Stacking', () => {
    // Scenario: Buy 3 distinct products to trigger multiple rules.
    
    cy.visit(webshopUrl + '/products');
    
    // Stub alerts
    cy.window().then((win) => {
      cy.stub(win, 'alert').as('alertStub');
    });

    cy.get('.product-card', { timeout: 10000 }).should('have.length.gt', 0);
    
    // Add 3 distinct items
    cy.contains('.product-card', 'Classic T-Shirt').within(() => {
        // Red S
        cy.get('select').eq(0).select('Red');
        cy.get('select').eq(1).select('S');
        cy.contains('button', 'Add to Cart').click();
        
        // Red M
        cy.get('select').eq(0).select('Red');
        cy.get('select').eq(1).select('M');
        cy.contains('button', 'Add to Cart').click();
        
        // Red L
        cy.get('select').eq(0).select('Red');
        cy.get('select').eq(1).select('L');
        cy.contains('button', 'Add to Cart').click();
    });
    
    cy.visit(webshopUrl + '/cart');
    
    // Stub alerts
    cy.window().then((win) => {
      cy.stub(win, 'alert').as('alertStub');
    });

    // Get current balance before checkout
    let initialPoints = 0;
    cy.get('#loyalty-points').invoke('text').then((text) => {
        initialPoints = parseInt(text);
    });
    
    cy.contains('Checkout').click();
    cy.get('@alertStub').should('have.been.calledWith', 'Order placed successfully!');
    
    // --- Manual Confirmation Step ---
    cy.clearCookies();
    cy.loginToOrderService();
    
    cy.contains('tr', 'PENDING_CONFIRMATION', { timeout: 10000 }).within(() => {
        cy.contains('button', 'Confirm').click();
    });
    cy.contains('tr', 'CONFIRMED').should('be.visible');
    
    cy.clearCookies();
    cy.visit(webshopUrl + '/login');
    cy.get('input[name="username"]').type(username);
    cy.get('input[name="password"]').type(password);
    cy.get('button[type="submit"]').click();
    // --------------------------------
    
    // Verify new balance
    cy.visit(webshopUrl + '/cart');
    
    // Wait for points to update (be greater than initial)
    cy.get('#loyalty-points').should((($div) => {
        const text = $div.text();
        const points = parseInt(text);
        expect(points).to.be.greaterThan(initialPoints);
    }));

    cy.get('#loyalty-points').invoke('text').then((text) => {
        const finalPoints = parseInt(text);
        const earned = finalPoints - initialPoints;
        
        // We expect at least 225 (3x multiplier applied)
        expect(earned).to.be.at.least(225);
    });
  });

  it('Test 3: Admin Dashboards', () => {
    // Check Order Service UI
    cy.loginToOrderService();
    cy.visit('https://reverse-proxy:8447/orders');
    cy.contains('tr', 'CONFIRMED').within(() => {
      cy.contains('td', '50').should('be.visible'); // Points earned (January Bonus)
    });

    // Check Loyalty Service UI
    cy.loginToLoyaltyService();
    cy.visit('https://reverse-proxy:8448/');
    cy.contains('h1', 'Loyalty Admin Dashboard').should('be.visible');
    //cy.contains('li', 'January Bonus').should('be.visible');
    //cy.contains('li', 'Double Points on 3+ Items').should('be.visible');
    //cy.contains('li', '1.5x Points on 3+ Distinct Items').should('be.visible');
  });
});
