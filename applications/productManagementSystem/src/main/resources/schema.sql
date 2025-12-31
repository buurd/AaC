DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS product_groups;

CREATE TABLE product_groups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    base_price NUMERIC(10, 2),
    base_unit VARCHAR(50)
);

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    group_id INTEGER REFERENCES product_groups(id),
    type VARCHAR(255),
    name VARCHAR(255),
    description TEXT,
    price NUMERIC(10, 2),
    unit VARCHAR(50),
    attributes TEXT
);

-- Insert sample data
INSERT INTO product_groups (name, description, base_price, base_unit)
VALUES ('Classic T-Shirt', '100% Cotton', 20.00, 'pcs');

-- Variants (Color: Red, Blue; Size: S, M, L) -> 6 Variants
INSERT INTO products (group_id, type, name, description, price, unit, attributes)
VALUES
(1, 'T-Shirt', 'Classic T-Shirt - Red S', '100% Cotton', 20.00, 'pcs', '{"Color": "Red", "Size": "S"}'),
(1, 'T-Shirt', 'Classic T-Shirt - Red M', '100% Cotton', 20.00, 'pcs', '{"Color": "Red", "Size": "M"}'),
(1, 'T-Shirt', 'Classic T-Shirt - Red L', '100% Cotton', 20.00, 'pcs', '{"Color": "Red", "Size": "L"}'),
(1, 'T-Shirt', 'Classic T-Shirt - Blue S', '100% Cotton', 20.00, 'pcs', '{"Color": "Blue", "Size": "S"}'),
(1, 'T-Shirt', 'Classic T-Shirt - Blue M', '100% Cotton', 20.00, 'pcs', '{"Color": "Blue", "Size": "M"}'),
(1, 'T-Shirt', 'Classic T-Shirt - Blue L', '100% Cotton', 20.00, 'pcs', '{"Color": "Blue", "Size": "L"}');
