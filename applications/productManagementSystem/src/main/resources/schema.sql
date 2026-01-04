CREATE TABLE IF NOT EXISTS product_groups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    base_price NUMERIC(10, 2),
    base_unit VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    group_id INTEGER REFERENCES product_groups(id),
    type VARCHAR(255),
    name VARCHAR(255),
    description TEXT,
    price NUMERIC(10, 2),
    unit VARCHAR(50),
    attributes TEXT
);

-- Group 1: T-Shirts
INSERT INTO product_groups (id, name, description, base_price, base_unit) VALUES (1, 'Classic T-Shirt', '100% Cotton, soft and comfortable', 25.00, 'pcs') ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, group_id, type, name, description, price, unit, attributes) VALUES
(1, 1, 'T-Shirt', 'Classic T-Shirt', '100% Cotton, soft and comfortable', 25.00, 'pcs', '{"Color": "Red", "Size": "S"}'),
(2, 1, 'T-Shirt', 'Classic T-Shirt', '100% Cotton, soft and comfortable', 25.00, 'pcs', '{"Color": "Red", "Size": "M"}'),
(3, 1, 'T-Shirt', 'Classic T-Shirt', '100% Cotton, soft and comfortable', 25.00, 'pcs', '{"Color": "Red", "Size": "L"}'),
(4, 1, 'T-Shirt', 'Classic T-Shirt', '100% Cotton, soft and comfortable', 25.00, 'pcs', '{"Color": "Blue", "Size": "S"}'),
(5, 1, 'T-Shirt', 'Classic T-Shirt', '100% Cotton, soft and comfortable', 25.00, 'pcs', '{"Color": "Blue", "Size": "M"}'),
(6, 1, 'T-Shirt', 'Classic T-Shirt', '100% Cotton, soft and comfortable', 25.00, 'pcs', '{"Color": "Blue", "Size": "L"}')
ON CONFLICT (id) DO NOTHING;

-- Group 2: Dessert Cups
INSERT INTO product_groups (id, name, description, base_price, base_unit) VALUES (2, 'Glass Dessert Cup', 'Elegant glass cups for your favorite desserts', 8.00, 'pcs') ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, group_id, type, name, description, price, unit, attributes) VALUES
(7, 2, 'Glassware', 'Glass Dessert Cup', 'Elegant glass cups for your favorite desserts', 8.00, 'pcs', '{"Pack Size": "1-pack"}'),
(8, 2, 'Glassware', 'Glass Dessert Cup', 'Elegant glass cups for your favorite desserts', 14.00, 'pcs', '{"Pack Size": "2-pack"}'),
(9, 2, 'Glassware', 'Glass Dessert Cup', 'Elegant glass cups for your favorite desserts', 25.00, 'pcs', '{"Pack Size": "4-pack"}')
ON CONFLICT (id) DO NOTHING;

-- Group 3: Coffee Beans
INSERT INTO product_groups (id, name, description, base_price, base_unit) VALUES (3, 'Artisan Coffee Beans', 'Single-origin, ethically sourced coffee beans', 15.00, 'g') ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, group_id, type, name, description, price, unit, attributes) VALUES
(10, 3, 'Coffee', 'Artisan Coffee Beans', 'Single-origin, ethically sourced coffee beans', 15.00, 'g', '{"Roast": "Light", "Weight": "250g"}'),
(11, 3, 'Coffee', 'Artisan Coffee Beans', 'Single-origin, ethically sourced coffee beans', 15.00, 'g', '{"Roast": "Medium", "Weight": "250g"}'),
(12, 3, 'Coffee', 'Artisan Coffee Beans', 'Single-origin, ethically sourced coffee beans', 28.00, 'g', '{"Roast": "Dark", "Weight": "500g"}'),
(13, 3, 'Coffee', 'Artisan Coffee Beans', 'Single-origin, ethically sourced coffee beans', 50.00, 'g', '{"Roast": "Dark", "Weight": "1kg"}')
ON CONFLICT (id) DO NOTHING;

-- Group 4: Wooden Bowl
INSERT INTO product_groups (id, name, description, base_price, base_unit) VALUES (4, 'Handcrafted Wooden Bowl', 'Beautiful and unique wooden bowls for your kitchen', 45.00, 'pcs') ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, group_id, type, name, description, price, unit, attributes) VALUES
(14, 4, 'Kitchenware', 'Handcrafted Wooden Bowl', 'Beautiful and unique wooden bowls for your kitchen', 45.00, 'pcs', '{"Wood": "Oak"}'),
(15, 4, 'Kitchenware', 'Handcrafted Wooden Bowl', 'Beautiful and unique wooden bowls for your kitchen', 55.00, 'pcs', '{"Wood": "Walnut"}')
ON CONFLICT (id) DO NOTHING;

-- Product 5: Standalone Laptop Stand
-- No group for this one.
INSERT INTO products (id, group_id, type, name, description, price, unit, attributes) VALUES
(16, NULL, 'Office', 'Ergonomic Laptop Stand', 'Adjustable aluminum laptop stand for better posture', 75.00, 'pcs', NULL)
ON CONFLICT (id) DO NOTHING;

-- Explicitly set sequence values to avoid conflicts with manual IDs
SELECT setval('product_groups_id_seq', (SELECT MAX(id) FROM product_groups));
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
