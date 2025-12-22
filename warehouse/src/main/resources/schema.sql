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

-- Seed Data
-- Product (matches PM ID 1)
INSERT INTO products (pm_id, name) VALUES (1, 'The Hitchhiker''s Guide to the Galaxy');

-- Delivery
INSERT INTO deliveries (sender) VALUES ('Initial Stock Supplier');

-- 10 Items for Product 1, Delivery 1
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 1, 'SN-001', 'New'),
(1, 1, 'SN-002', 'New'),
(1, 1, 'SN-003', 'New'),
(1, 1, 'SN-004', 'New'),
(1, 1, 'SN-005', 'New'),
(1, 1, 'SN-006', 'New'),
(1, 1, 'SN-007', 'New'),
(1, 1, 'SN-008', 'New'),
(1, 1, 'SN-009', 'New'),
(1, 1, 'SN-010', 'New');
