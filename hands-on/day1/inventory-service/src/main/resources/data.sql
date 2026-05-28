-- Seed data for inventory-service
-- Loaded automatically by Spring Boot on startup (schema created by Hibernate)

INSERT INTO product (id, name, sku, stock_quantity, reserved_quantity, unit_price) VALUES
    (1, 'Wireless Headphones', 'SKU-WH-001', 150, 0, 79.99),
    (2, 'Mechanical Keyboard',  'SKU-KB-002', 75,  0, 129.99),
    (3, 'USB-C Hub',            'SKU-HB-003', 200, 0, 39.99),
    (4, 'Webcam HD 1080p',      'SKU-WC-004', 60,  0, 89.99),
    (5, 'Monitor Stand',        'SKU-MS-005', 40,  0, 49.99);
