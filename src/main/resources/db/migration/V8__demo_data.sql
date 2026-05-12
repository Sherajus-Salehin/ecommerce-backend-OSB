
INSERT INTO categories (id, name, description, code, is_active, created_at, modified_at, created_by, modified_by)
VALUES
(1, 'Electronics', 'electronics c description', 'ELEC', true, now(), now(), 1, 1),
(2, 'Books', 'book c description', 'BOOK', true, now(), now(), 1, 1);

INSERT INTO products (id, sku, name, description, price, is_active, category_id, image_url, created_at, modified_at, created_by, modified_by)
VALUES
(1, 'ELEC-001', 'Wireless Headphones','headphones p descrription', 199.99, true, 1, NULL, now(), now(), 1, 1),
(2, 'BOOK-001', 'Software Engineering to farming 101', 'Exist kora uchit', 29.99, true, 2, NULL, now(), now(), 1, 1);

INSERT INTO inventories (product_id, total_quantity, reserved_quantity, version, created_at, modified_at, created_by, modified_by)
VALUES
(1, 50, 5, 0, now(), now(), 1, 1),
(2, 100, 20, 0, now(), now(), 1, 1);

INSERT INTO coupons (code, discount, max_discount, created_at, modified_at, created_by, modified_by, is_active)
VALUES
('DESH10', 10.0, 100.0, now(), now(), 1, 1, true),
('NINE11', 11.0, 200.0, now(), now(), 1, 1, true),--😶
('MIND20', 20.0, 1000.0, now(), now(), 1, 1, true);

INSERT INTO carts (id, user_id, created_at, modified_at, created_by, modified_by)
VALUES
(1, 3, now(), now(), 3, 3),
(2, 4, now(), now(), 4, 4);

INSERT INTO cart_items (cart_id, product_id, quantity, unit_price)
VALUES
(1, 1, 1, 199.99),
(1, 2, 2, 29.99),
(2, 2, 1, 29.99),
(2, 1, 1, 199.99);

-- orders
INSERT INTO orders (id, order_number, user_id, status, total_amount, cancelled_at, created_at, modified_at, created_by, modified_by)
VALUES
(1, '3fa85f64-5717-4562-b3fc-2c963f66afa6', 3, 'PAID', 259.97, NULL, now(), now(), 3, 3),
(2, '4fa85f64-5717-4562-b3fc-2c963f66afa6', 4, 'CREATED', 229.98, NULL, now(), now(), 4, 4);

INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, total_price)
VALUES
(1, 1, 'Wireless Headphones', 199.99, 1, 199.99),
(1, 2, 'Software Engineering to farming 101', 29.99, 2, 59.98),
(2, 2, 'Software Engineering to farming 101', 29.99, 2, 59.98),
(2, 1, 'Wireless Headphones', 199.99, 1, 199.99);