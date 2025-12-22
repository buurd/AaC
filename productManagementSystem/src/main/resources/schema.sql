CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    type VARCHAR(255),
    name VARCHAR(255),
    description TEXT,
    price NUMERIC(10, 2),
    unit VARCHAR(50)
);

-- Insert sample data
-- ID 1
INSERT INTO products (type, name, description, price, unit)
VALUES ('Book', 'The Hitchhiker''s Guide to the Galaxy', 'A sci-fi comedy classic.', 12.50, 'pcs');
