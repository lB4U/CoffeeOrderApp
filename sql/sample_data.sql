-- Insert sample users
INSERT INTO users (username, password, role) VALUES
('admin', 'admin123', 'ADMIN'),
('cashier', 'cashier123', 'CASHIER');

-- Insert sample products
INSERT INTO products (name, image_name, price, stock, category) VALUES
('Espresso', 'espresso.png', 2.50, 100, 'DRINK'),
('Cappuccino', 'cappuccino.png', 3.50, 80, 'DRINK'),
('Cheesecake', 'cheesecake.png', 4.00, 20, 'DESSERT'),
('Americano', 'americano.png', 2.00, 90, 'DRINK'),
('Flat White', 'flatwhite.png', 3.00, 60, 'DRINK'),
('Latte', 'latte.png', 3.80, 70, 'DRINK'),
('Matcha', 'matcha.png', 4.50, 50, 'DRINK'),
('V60', 'v60.png', 4.00, 40, 'DRINK'),
('Brownie', 'Brownie.jpg', 2.50, 30, 'DESSERT'),
('Default Product', 'default.png', 1.00, 100, 'DESSERT');

-- Insert sample discount codes
INSERT INTO discount_codes (code, discount_percent, active) VALUES
('WELCOME10', 0.10, TRUE),
('SUMMER20', 0.20, TRUE),
('50OFF', 0.50, FALSE);



