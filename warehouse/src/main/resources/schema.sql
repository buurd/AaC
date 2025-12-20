CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    pm_id INTEGER UNIQUE,
    name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS inventory (
    id SERIAL PRIMARY KEY,
    product_id INTEGER,
    quantity INTEGER,
    location VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS deliveries (
    id SERIAL PRIMARY KEY,
    sender VARCHAR(255),
    delivery_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_individuals (
    id SERIAL PRIMARY KEY,
    delivery_id INTEGER REFERENCES deliveries(id) ON DELETE CASCADE,
    product_id INTEGER, -- References local product ID
    serial_number VARCHAR(255),
    state VARCHAR(50) -- e.g., 'New', 'Damaged'
);

-- Insert sample data
INSERT INTO products (pm_id, name) VALUES (1, 'Sample Product') ON CONFLICT DO NOTHING;
INSERT INTO inventory (product_id, quantity, location) VALUES (1, 100, 'A-1-1') ON CONFLICT DO NOTHING;
