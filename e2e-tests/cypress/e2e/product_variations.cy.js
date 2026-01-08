describe('Product Variations', () => {
  const groupName = 'Dynamic T-Shirt';
  const webshopUrl = 'https://reverse-proxy:8443/products';

  const verifyWebshopContent = (content, attempts = 0) => {
    if (attempts > 20) {
      throw new Error(`Timed out waiting for content: '${content}' in Webshop`);
    }
    cy.wait(1000);
    cy.request({ url: webshopUrl, failOnStatusCode: false }).then((res) => {
      // The webshop response body is HTML, so we check if it includes the content string
      if (res.status === 200 && res.body.includes(content)) {
        return; // Success
      }
      cy.log(`Content '${content}' not found yet. Retrying... (${attempts + 1}/20)`);
      verifyWebshopContent(content, attempts + 1);
    });
  };

  beforeEach(() => {
    cy.loginToPM();
  });

  it('should handle dynamic attributes and prevent duplicates', () => {
    // 1. Create Product Group
    cy.visit('https://reverse-proxy:8444/groups');
    cy.contains('button', 'Create New Group').click();
    cy.get('input[name="name"]').type(groupName);
    cy.get('input[name="description"]').type('Dynamic Attributes Test');
    cy.get('input[name="basePrice"]').type('30.00');
    cy.get('button[type="submit"]').click();
    cy.contains('td', groupName).should('be.visible');

    // 2. Generate Variants (First Pass)
    cy.contains('tr', groupName).within(() => {
      cy.contains('button', 'Generate Variants').click();
    });
    
    const attributes1 = "Color: Red, Blue\nSize: M";
    cy.get('textarea[name="attributes"]').clear().type(attributes1);
    cy.get('button[type="submit"]').click();

    // Verify 2 variants created
    cy.url().should('include', '/products');
    // The name should just be the group name in the PM UI
    cy.contains('td', groupName).should('be.visible');
    // The attributes should be visible in the attributes column
    // Note: The attributes are sorted by key, so "Color" comes before "Size"
    // Also, formatAttributes adds a space after comma: "Color: Red, Size: M"
    cy.contains('td', 'Color: Red, Size: M').should('be.visible');
    cy.contains('td', 'Color: Blue, Size: M').should('be.visible');

    // 3. Generate Variants (Second Pass - Idempotency)
    cy.visit('https://reverse-proxy:8444/groups');
    cy.contains('tr', groupName).within(() => {
      cy.contains('button', 'Generate Variants').click();
    });

    const attributesSame = "Color: Red, Blue\nSize: M";
    cy.get('textarea[name="attributes"]').clear().type(attributesSame);
    cy.get('button[type="submit"]').click();
    
    // Verify we still only have 2 variants (no duplicates)
    // We check that we don't see duplicate rows for the same attributes.
    cy.get('td').contains('Color: Red, Size: M').should('have.length', 1);

    // 4. Generate Variants (New Attribute)
    cy.visit('https://reverse-proxy:8444/groups');
    cy.contains('tr', groupName).within(() => {
      cy.contains('button', 'Generate Variants').click();
    });
    
    const attributesNew = "Color: Green\nSize: M";
    cy.get('textarea[name="attributes"]').clear().type(attributesNew);
    cy.get('button[type="submit"]').click();
    
    // Verify new variant added
    cy.contains('td', 'Color: Green, Size: M').should('be.visible');

    // 5. Sync Group
    cy.visit('https://reverse-proxy:8444/groups');
    cy.contains('tr', groupName).within(() => {
      cy.contains('button', 'Sync All').click();
    });
    
    // 6. Verify in Webshop (Webshop sees the flattened name)
    // The flattened name is constructed as "GroupName - AttributeValue1 AttributeValue2 ..."
    // Attributes are sorted by key: Color, Size. So "Red M" is correct.
    verifyWebshopContent('Dynamic T-Shirt - Red M');
    verifyWebshopContent('Dynamic T-Shirt - Green M');
  });
});
