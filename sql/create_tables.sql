-- Create the users table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL,
    role VARCHAR(10) NOT NULL -- ADMIN or CASHIER
);

-- Create the products table
CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    image_name VARCHAR(100) DEFAULT 'default.png',
    price DOUBLE NOT NULL,
    stock INT NOT NULL,
    category VARCHAR(10) NOT NULL -- DRINK or DESSERT
);

-- Create the discount_codes table
CREATE TABLE discount_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    discount_percent DOUBLE NOT NULL, -- Value between 0.0 and 1.0
    usage_count INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE
);

-- Create the invoices table
CREATE TABLE invoices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(20) NOT NULL, -- Unique reference
    date_time DATETIME NOT NULL,
    total DOUBLE NOT NULL
);

-- Create the invoice_items table
CREATE TABLE invoice_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_id INT NOT NULL,
    product_name VARCHAR(100) NOT NULL, -- Product name reference
    quantity INT NOT NULL,
    line_price DOUBLE NOT NULL,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

