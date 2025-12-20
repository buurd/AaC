CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    pm_id INTEGER UNIQUE,
    type VARCHAR(255),
    name VARCHAR(255),
    description TEXT,
    price NUMERIC(10, 2),
    unit VARCHAR(50)
);

-- Insert some sample data (local only, no pm_id)
INSERT INTO products (type, name, description, price, unit)
SELECT 'Book', 'The Hitchhiker''s Guide to the Galaxy', 'A sci-fi comedy classic.', 12.50, 'pcs'
WHERE NOT EXISTS (SELECT 1 FROM products);
