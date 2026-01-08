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

-- Seed Data using ON CONFLICT to avoid duplicates
INSERT INTO products (pm_id, type, name, description, price, unit, stock) VALUES
(1, 'T-Shirt', 'Classic T-Shirt - Red S', '100% Cotton, soft and comfortable', 25.00, 'pcs', 5),
(2, 'T-Shirt', 'Classic T-Shirt - Red M', '100% Cotton, soft and comfortable', 25.00, 'pcs', 8),
(3, 'T-Shirt', 'Classic T-Shirt - Red L', '100% Cotton, soft and comfortable', 25.00, 'pcs', 2),
(4, 'T-Shirt', 'Classic T-Shirt - Blue S', '100% Cotton, soft and comfortable', 25.00, 'pcs', 4),
(5, 'T-Shirt', 'Classic T-Shirt - Blue M', '100% Cotton, soft and comfortable', 25.00, 'pcs', 6),
(6, 'T-Shirt', 'Classic T-Shirt - Blue L', '100% Cotton, soft and comfortable', 25.00, 'pcs', 3),
(7, 'Glassware', 'Glass Dessert Cup - 1-pack', 'Elegant glass cups for your favorite desserts', 8.00, 'pcs', 20),
(8, 'Glassware', 'Glass Dessert Cup - 2-pack', 'Elegant glass cups for your favorite desserts', 14.00, 'pcs', 10),
(9, 'Glassware', 'Glass Dessert Cup - 4-pack', 'Elegant glass cups for your favorite desserts', 25.00, 'pcs', 5),
(10, 'Coffee', 'Artisan Coffee Beans - Light 250g', 'Single-origin, ethically sourced coffee beans', 15.00, 'g', 15),
(11, 'Coffee', 'Artisan Coffee Beans - Medium 250g', 'Single-origin, ethically sourced coffee beans', 15.00, 'g', 15),
(12, 'Coffee', 'Artisan Coffee Beans - Dark 500g', 'Single-origin, ethically sourced coffee beans', 28.00, 'g', 10),
(13, 'Coffee', 'Artisan Coffee Beans - Dark 1kg', 'Single-origin, ethically sourced coffee beans', 50.00, 'g', 5),
(14, 'Kitchenware', 'Handcrafted Wooden Bowl - Oak', 'Beautiful and unique wooden bowls for your kitchen', 45.00, 'pcs', 3),
(15, 'Kitchenware', 'Handcrafted Wooden Bowl - Walnut', 'Beautiful and unique wooden bowls for your kitchen', 55.00, 'pcs', 2),
(16, 'Office', 'Ergonomic Laptop Stand', 'Adjustable aluminum laptop stand for better posture', 75.00, 'pcs', 10)
ON CONFLICT (pm_id) DO NOTHING;
