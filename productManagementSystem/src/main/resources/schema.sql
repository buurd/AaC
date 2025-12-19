CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    type VARCHAR(255),
    name VARCHAR(255),
    description TEXT,
    price NUMERIC(10, 2),
    unit VARCHAR(50)
);

-- Insert some sample data for PM System
INSERT INTO products (type, name, description, price, unit)
SELECT 'Tool', 'Hammer', 'A heavy hammer.', 15.00, 'pcs'
WHERE NOT EXISTS (SELECT 1 FROM products);

INSERT INTO products (type, name, description, price, unit)
SELECT 'Tool', 'Screwdriver', 'A flathead screwdriver.', 5.00, 'pcs'
WHERE NOT EXISTS (SELECT 1 FROM products);
