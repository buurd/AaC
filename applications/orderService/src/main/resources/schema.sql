CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    customer_name VARCHAR(255),
    status VARCHAR(50), -- PENDING, CONFIRMED, REJECTED
    total_amount DECIMAL(10, 2) DEFAULT 0.00,
    points_redeemed INT DEFAULT 0,
    points_earned INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id) ON DELETE CASCADE,
    product_id INTEGER,
    quantity INTEGER
);

CREATE TABLE IF NOT EXISTS invoices (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id) ON DELETE CASCADE,
    customer_name VARCHAR(255),
    amount DECIMAL(10, 2),
    due_date DATE,
    paid BOOLEAN DEFAULT FALSE
);
