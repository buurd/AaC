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
-- Products (matching PM IDs 1-6)
INSERT INTO products (pm_id, name) VALUES
(1, 'Classic T-Shirt - Red S'),
(2, 'Classic T-Shirt - Red M'),
(3, 'Classic T-Shirt - Red L'),
(4, 'Classic T-Shirt - Blue S'),
(5, 'Classic T-Shirt - Blue M'),
(6, 'Classic T-Shirt - Blue L');

-- Delivery
INSERT INTO deliveries (sender) VALUES ('Initial Stock Supplier');

-- Items for Product 1 (Red S) - 5 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 1, 'SN-RED-S-001', 'New'),
(1, 1, 'SN-RED-S-002', 'New'),
(1, 1, 'SN-RED-S-003', 'New'),
(1, 1, 'SN-RED-S-004', 'New'),
(1, 1, 'SN-RED-S-005', 'New');

-- Items for Product 2 (Red M) - 8 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 2, 'SN-RED-M-001', 'New'),
(1, 2, 'SN-RED-M-002', 'New'),
(1, 2, 'SN-RED-M-003', 'New'),
(1, 2, 'SN-RED-M-004', 'New'),
(1, 2, 'SN-RED-M-005', 'New'),
(1, 2, 'SN-RED-M-006', 'New'),
(1, 2, 'SN-RED-M-007', 'New'),
(1, 2, 'SN-RED-M-008', 'New');

-- Items for Product 3 (Red L) - 2 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 3, 'SN-RED-L-001', 'New'),
(1, 3, 'SN-RED-L-002', 'New');

-- Items for Product 4 (Blue S) - 4 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 4, 'SN-BLUE-S-001', 'New'),
(1, 4, 'SN-BLUE-S-002', 'New'),
(1, 4, 'SN-BLUE-S-003', 'New'),
(1, 4, 'SN-BLUE-S-004', 'New');

-- Items for Product 5 (Blue M) - 6 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 5, 'SN-BLUE-M-001', 'New'),
(1, 5, 'SN-BLUE-M-002', 'New'),
(1, 5, 'SN-BLUE-M-003', 'New'),
(1, 5, 'SN-BLUE-M-004', 'New'),
(1, 5, 'SN-BLUE-M-005', 'New'),
(1, 5, 'SN-BLUE-M-006', 'New');

-- Items for Product 6 (Blue L) - 3 items
INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES
(1, 6, 'SN-BLUE-L-001', 'New'),
(1, 6, 'SN-BLUE-L-002', 'New'),
(1, 6, 'SN-BLUE-L-003', 'New');
