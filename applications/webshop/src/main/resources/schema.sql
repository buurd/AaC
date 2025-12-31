CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    pm_id INTEGER UNIQUE,
    type VARCHAR(255),
    name VARCHAR(255),
    description TEXT,
    price NUMERIC(10, 2),
    unit VARCHAR(50),
    stock INTEGER DEFAULT 0
);

-- Seed Data
-- Matches PM IDs 1-6 and Warehouse Stock levels
INSERT INTO products (pm_id, type, name, description, price, unit, stock) VALUES
(1, 'T-Shirt', 'Classic T-Shirt - Red S', '100% Cotton', 20.00, 'pcs', 5),
(2, 'T-Shirt', 'Classic T-Shirt - Red M', '100% Cotton', 20.00, 'pcs', 8),
(3, 'T-Shirt', 'Classic T-Shirt - Red L', '100% Cotton', 20.00, 'pcs', 2),
(4, 'T-Shirt', 'Classic T-Shirt - Blue S', '100% Cotton', 20.00, 'pcs', 4),
(5, 'T-Shirt', 'Classic T-Shirt - Blue M', '100% Cotton', 20.00, 'pcs', 6),
(6, 'T-Shirt', 'Classic T-Shirt - Blue L', '100% Cotton', 20.00, 'pcs', 3);
