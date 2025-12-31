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
-- Matches PM ID 1 and Warehouse Stock 10
INSERT INTO products (pm_id, type, name, description, price, unit, stock)
VALUES (1, 'Book', 'The Hitchhiker''s Guide to the Galaxy', 'A sci-fi comedy classic.', 12.50, 'pcs', 10);
