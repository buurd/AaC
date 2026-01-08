CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    pm_id INTEGER UNIQUE,
    name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS deliveries (
    id SERIAL PRIMARY KEY,
    sender VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS product_individuals (
    id SERIAL PRIMARY KEY,
    delivery_id INTEGER REFERENCES deliveries(id) ON DELETE CASCADE,
    product_id INTEGER REFERENCES products(id),
    serial_number VARCHAR(255),
    state VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS fulfillment_orders (
    id SERIAL PRIMARY KEY,
    order_id INTEGER UNIQUE,
    status VARCHAR(50) DEFAULT 'PENDING'
);

-- Seed Data
-- Products (matching PM IDs 1-16)
INSERT INTO products (pm_id, name) VALUES
(1, 'Classic T-Shirt - Red S'),
(2, 'Classic T-Shirt - Red M'),
(3, 'Classic T-Shirt - Red L'),
(4, 'Classic T-Shirt - Blue S'),
(5, 'Classic T-Shirt - Blue M'),
(6, 'Classic T-Shirt - Blue L'),
(7, 'Glass Dessert Cup - 1-pack'),
(8, 'Glass Dessert Cup - 2-pack'),
(9, 'Glass Dessert Cup - 4-pack'),
(10, 'Artisan Coffee Beans - Light 250g'),
(11, 'Artisan Coffee Beans - Medium 250g'),
(12, 'Artisan Coffee Beans - Dark 500g'),
(13, 'Artisan Coffee Beans - Dark 1kg'),
(14, 'Handcrafted Wooden Bowl - Oak'),
(15, 'Handcrafted Wooden Bowl - Walnut'),
(16, 'Ergonomic Laptop Stand')
ON CONFLICT (pm_id) DO NOTHING;

-- Delivery
INSERT INTO deliveries (id, sender) VALUES (1, 'Initial Stock Supplier') ON CONFLICT (id) DO NOTHING;

-- Items for Product 1 (Red S) - 5 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 1, 'SN-RED-S-001', 'New'), (1, 1, 'SN-RED-S-002', 'New'), (1, 1, 'SN-RED-S-003', 'New'), (1, 1, 'SN-RED-S-004', 'New'), (1, 1, 'SN-RED-S-005', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 2 (Red M) - 8 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 2, 'SN-RED-M-001', 'New'), (1, 2, 'SN-RED-M-002', 'New'), (1, 2, 'SN-RED-M-003', 'New'), (1, 2, 'SN-RED-M-004', 'New'), (1, 2, 'SN-RED-M-005', 'New'), (1, 2, 'SN-RED-M-006', 'New'), (1, 2, 'SN-RED-M-007', 'New'), (1, 2, 'SN-RED-M-008', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 3 (Red L) - 2 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 3, 'SN-RED-L-001', 'New'), (1, 3, 'SN-RED-L-002', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 4 (Blue S) - 4 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 4, 'SN-BLUE-S-001', 'New'), (1, 4, 'SN-BLUE-S-002', 'New'), (1, 4, 'SN-BLUE-S-003', 'New'), (1, 4, 'SN-BLUE-S-004', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 5 (Blue M) - 6 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 5, 'SN-BLUE-M-001', 'New'), (1, 5, 'SN-BLUE-M-002', 'New'), (1, 5, 'SN-BLUE-M-003', 'New'), (1, 5, 'SN-BLUE-M-004', 'New'), (1, 5, 'SN-BLUE-M-005', 'New'), (1, 5, 'SN-BLUE-M-006', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 6 (Blue L) - 3 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 6, 'SN-BLUE-L-001', 'New'), (1, 6, 'SN-BLUE-L-002', 'New'), (1, 6, 'SN-BLUE-L-003', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 7 (Dessert Cup 1-pack) - 20 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 7, 'SN-CUP-1-001', 'New'), (1, 7, 'SN-CUP-1-002', 'New'), (1, 7, 'SN-CUP-1-003', 'New'), (1, 7, 'SN-CUP-1-004', 'New'), (1, 7, 'SN-CUP-1-005', 'New'),
(1, 7, 'SN-CUP-1-006', 'New'), (1, 7, 'SN-CUP-1-007', 'New'), (1, 7, 'SN-CUP-1-008', 'New'), (1, 7, 'SN-CUP-1-009', 'New'), (1, 7, 'SN-CUP-1-010', 'New'),
(1, 7, 'SN-CUP-1-011', 'New'), (1, 7, 'SN-CUP-1-012', 'New'), (1, 7, 'SN-CUP-1-013', 'New'), (1, 7, 'SN-CUP-1-014', 'New'), (1, 7, 'SN-CUP-1-015', 'New'),
(1, 7, 'SN-CUP-1-016', 'New'), (1, 7, 'SN-CUP-1-017', 'New'), (1, 7, 'SN-CUP-1-018', 'New'), (1, 7, 'SN-CUP-1-019', 'New'), (1, 7, 'SN-CUP-1-020', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 8 (Dessert Cup 2-pack) - 10 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 8, 'SN-CUP-2-001', 'New'), (1, 8, 'SN-CUP-2-002', 'New'), (1, 8, 'SN-CUP-2-003', 'New'), (1, 8, 'SN-CUP-2-004', 'New'), (1, 8, 'SN-CUP-2-005', 'New'),
(1, 8, 'SN-CUP-2-006', 'New'), (1, 8, 'SN-CUP-2-007', 'New'), (1, 8, 'SN-CUP-2-008', 'New'), (1, 8, 'SN-CUP-2-009', 'New'), (1, 8, 'SN-CUP-2-010', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 9 (Dessert Cup 4-pack) - 5 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 9, 'SN-CUP-4-001', 'New'), (1, 9, 'SN-CUP-4-002', 'New'), (1, 9, 'SN-CUP-4-003', 'New'), (1, 9, 'SN-CUP-4-004', 'New'), (1, 9, 'SN-CUP-4-005', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 10 (Coffee Light 250g) - 15 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 10, 'SN-COF-L250-001', 'New'), (1, 10, 'SN-COF-L250-002', 'New'), (1, 10, 'SN-COF-L250-003', 'New'), (1, 10, 'SN-COF-L250-004', 'New'), (1, 10, 'SN-COF-L250-005', 'New'),
(1, 10, 'SN-COF-L250-006', 'New'), (1, 10, 'SN-COF-L250-007', 'New'), (1, 10, 'SN-COF-L250-008', 'New'), (1, 10, 'SN-COF-L250-009', 'New'), (1, 10, 'SN-COF-L250-010', 'New'),
(1, 10, 'SN-COF-L250-011', 'New'), (1, 10, 'SN-COF-L250-012', 'New'), (1, 10, 'SN-COF-L250-013', 'New'), (1, 10, 'SN-COF-L250-014', 'New'), (1, 10, 'SN-COF-L250-015', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 11 (Coffee Medium 250g) - 15 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 11, 'SN-COF-M250-001', 'New'), (1, 11, 'SN-COF-M250-002', 'New'), (1, 11, 'SN-COF-M250-003', 'New'), (1, 11, 'SN-COF-M250-004', 'New'), (1, 11, 'SN-COF-M250-005', 'New'),
(1, 11, 'SN-COF-M250-006', 'New'), (1, 11, 'SN-COF-M250-007', 'New'), (1, 11, 'SN-COF-M250-008', 'New'), (1, 11, 'SN-COF-M250-009', 'New'), (1, 11, 'SN-COF-M250-010', 'New'),
(1, 11, 'SN-COF-M250-011', 'New'), (1, 11, 'SN-COF-M250-012', 'New'), (1, 11, 'SN-COF-M250-013', 'New'), (1, 11, 'SN-COF-M250-014', 'New'), (1, 11, 'SN-COF-M250-015', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 12 (Coffee Dark 500g) - 10 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 12, 'SN-COF-D500-001', 'New'), (1, 12, 'SN-COF-D500-002', 'New'), (1, 12, 'SN-COF-D500-003', 'New'), (1, 12, 'SN-COF-D500-004', 'New'), (1, 12, 'SN-COF-D500-005', 'New'),
(1, 12, 'SN-COF-D500-006', 'New'), (1, 12, 'SN-COF-D500-007', 'New'), (1, 12, 'SN-COF-D500-008', 'New'), (1, 12, 'SN-COF-D500-009', 'New'), (1, 12, 'SN-COF-D500-010', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 13 (Coffee Dark 1kg) - 5 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 13, 'SN-COF-D1KG-001', 'New'), (1, 13, 'SN-COF-D1KG-002', 'New'), (1, 13, 'SN-COF-D1KG-003', 'New'), (1, 13, 'SN-COF-D1KG-004', 'New'), (1, 13, 'SN-COF-D1KG-005', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 14 (Wooden Bowl Oak) - 3 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 14, 'SN-BOWL-OAK-001', 'New'), (1, 14, 'SN-BOWL-OAK-002', 'New'), (1, 14, 'SN-BOWL-OAK-003', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 15 (Wooden Bowl Walnut) - 2 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 15, 'SN-BOWL-WAL-001', 'New'), (1, 15, 'SN-BOWL-WAL-002', 'New')
ON CONFLICT DO NOTHING;

-- Items for Product 16 (Laptop Stand) - 10 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 16, 'SN-LAPTOP-001', 'New'), (1, 16, 'SN-LAPTOP-002', 'New'), (1, 16, 'SN-LAPTOP-003', 'New'), (1, 16, 'SN-LAPTOP-004', 'New'), (1, 16, 'SN-LAPTOP-005', 'New'),
(1, 16, 'SN-LAPTOP-006', 'New'), (1, 16, 'SN-LAPTOP-007', 'New'), (1, 16, 'SN-LAPTOP-008', 'New'), (1, 16, 'SN-LAPTOP-009', 'New'), (1, 16, 'SN-LAPTOP-010', 'New')
ON CONFLICT DO NOTHING;

-- Explicitly set sequence values to avoid conflicts with manual IDs
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('deliveries_id_seq', (SELECT MAX(id) FROM deliveries));
SELECT setval('product_individuals_id_seq', (SELECT MAX(id) FROM product_individuals));
