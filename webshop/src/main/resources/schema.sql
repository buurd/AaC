CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    type VARCHAR(255),
    name VARCHAR(255),
    description TEXT,
    price NUMERIC(10, 2),
    unit VARCHAR(50)
);

-- Insert some sample data, but only if the table is empty
INSERT INTO products (type, name, description, price, unit)
SELECT 'Book', 'The Hitchhiker''s Guide to the Galaxy', 'A sci-fi comedy classic.', 12.50, 'pcs'
WHERE NOT EXISTS (SELECT 1 FROM products);

INSERT INTO products (type, name, description, price, unit)
SELECT 'Book', 'The Lord of the Rings', 'An epic fantasy adventure.', 25.00, 'pcs'
WHERE NOT EXISTS (SELECT 1 FROM products);

INSERT INTO products (type, name, description, price, unit)
SELECT 'Food', 'Apple', 'A crisp and juicy apple.', 0.50, 'kg'
WHERE NOT EXISTS (SELECT 1 FROM products);
