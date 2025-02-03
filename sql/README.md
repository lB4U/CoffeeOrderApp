# Database Setup for CoffeeOrderApp ☕

This guide explains how to set up the MySQL database for the CoffeeOrderApp project.

---

## **1. Database Configuration**
- **Database Name**: `CoffeeOrderDB`
- **Required Tables**:
  - `users`
  - `products`
  - `discount_codes`
  - `invoices`
  - `invoice_items`

---

## **2. Creating the Database**

1. **Login to MySQL**:
   Open your terminal or MySQL client and log in using your MySQL credentials:
   ```bash
   mysql -u <username> -p
   ```
   Replace `<username>` with your MySQL username e.g. mysql -u root -p, and enter your password when prompted.

2. **Create the Database**:
   ```sql
   CREATE DATABASE CoffeeOrderDB;
   USE CoffeeOrderDB;
   ```

---

## **3. Creating the Tables**

Copy and execute the SQL script below to create the required tables.

### `create_tables.sql`:
```sql
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
```

---

## **4. Inserting Sample Data**

### `sample_data.sql`:
Use the following script to populate the tables with initial data.

```sql
-- Insert sample users
INSERT INTO users (username, password, role) VALUES
('admin', 'admin', 'ADMIN'),
('cashier', 'cashier', 'CASHIER');

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
```

---

## **5. Connecting the Application to the Database**

1. Open the `CoffeeOrderApp.java` file.
2. Locate the `initDatabase()` method and update the following variables to match your local environment:
   ```java
   String url = "jdbc:mysql://localhost:3306/CoffeeOrderDB?useSSL=false&serverTimezone=UTC";
   String user = "your_username";
   String password = "your_password";
   ```

---

## **6. Verify the Setup**

1. After creating the database and inserting the data, use a MySQL client or query tool to verify the tables and data:
   ```sql
   SHOW TABLES;
   SELECT * FROM products;
   SELECT * FROM discount_codes;
   ```

2. Run the CoffeeOrderApp and test functionality:
   - Log in as `admin` to manage inventory and discounts.
   - Log in as `cashier` to place orders and apply discounts.

---

## **7. Troubleshooting**

- **Issue**: Cannot connect to the database.
  - Ensure the MySQL server is running.
  - Verify the connection URL, username, and password in the `initDatabase()` method.

- **Issue**: Tables not found.
  - Ensure you executed the `create_tables.sql` script in the correct database.

- **Issue**: Missing images.
  - Verify that all image files are placed in the `build/classes/App/images` directory.

---

## **8. Notes**

- Use **phpMyAdmin**, **MySQL Workbench**, or any preferred MySQL GUI to manage the database visually.
- For production, ensure proper security measures, such as hashing passwords.

---

### Enjoy building your Coffee Order App! ☕
