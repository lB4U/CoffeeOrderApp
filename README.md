## **CoffeeOrderApp (A JavaFX-Based Coffee Ordering System) ☕**

---

## 1. Introduction

This project is a JavaFX application that simulates a coffee shop's order management system. It supports two user roles: 
- **ADMIN**: Can manage inventory, discount codes, and view reports.
- **CASHIER**: Can create orders and apply discount codes.

The app connects to a MySQL database where products, invoices, and discount codes are stored.

---

## 2. Features

1. **Login & Authentication**  
   - Different roles: `ADMIN` or `CASHIER`.
   - Password-based login.
   - Role-based UI display (admin sidebar vs. cashier sidebar).

2. **Ordering & Invoicing**  
   - Browse products by category (Drinks / Dessert / All).
   - Use advanced search (by product name and min/max price).
   - Easily add items to the invoice with quantity adjustments.
   - Complete an order and optionally print the receipt.
   - Invoices (past orders) can be viewed with item breakdowns.

3. **Discount Codes**  
   - Cashiers can apply discount codes (percentage-based) to the current invoice.
   - Administrators can create, edit, or delete discount codes in the database.

4. **Inventory Management** (Admin only)  
   - Add, edit, or delete products.
   - Set product images by drag-and-drop or file chooser.
   - Update product price, stock, and category in the DB.

5. **Reports** (Admin only)  
   - **Daily Sales**: Summarize each day’s total sales.
   - **Top-Selling Products**: Display a bar chart of top-selling products.
   - **Low-Stock Items**: List products with stock below a set threshold (e.g., less than 5).

6. **Printing**  
   - When completing an order, you can choose to print the receipt using JavaFX’s `PrinterJob`.

---

## 3. Requirements

- **Java 8 or later** (Java 11+ recommended).
- **JavaFX SDK** (for Java 8, it’s often included in the JDK; for Java 11+, you might need a standalone JavaFX runtime).
- **MySQL** database instance or server.
- **Maven** or **Gradle** (optional) for easier dependency management.  
  *(You can also run it with a direct classpath configuration if you have the JavaFX libraries available.)*

> **Note**: The code references a stylesheet `coffee_style.css` and a default image located at `/App/coffee_style.css` and `/App/images/default.png` in the classpath. Ensure that these resources are correctly placed in your project structure.

---

## 4. Database Setup

**Schema Name**: `CoffeeOrderDB` (as referenced in the code)  
**Tables**:
1. **users**  
   ```sql
   CREATE TABLE users (
       id INT AUTO_INCREMENT PRIMARY KEY,
       username VARCHAR(50) NOT NULL,
       password VARCHAR(50) NOT NULL,
       role VARCHAR(10) NOT NULL  -- e.g., "ADMIN" or "CASHIER"
   );
   ```
   > Insert some default users:
   ```sql
   INSERT INTO users (username, password, role) VALUES
     ('admin', 'admin123', 'ADMIN'),
     ('cashier', 'cashier123', 'CASHIER');
   ```

2. **products**  
   ```sql
   CREATE TABLE products (
       id INT AUTO_INCREMENT PRIMARY KEY,
       name VARCHAR(100) NOT NULL,
       image_name VARCHAR(100) DEFAULT 'default.png',
       price DOUBLE NOT NULL,
       stock INT NOT NULL,
       category VARCHAR(10) NOT NULL  -- e.g., "DRINK" or "DESSERT"
   );
   ```
   > Optionally insert some sample products:
   ```sql
   INSERT INTO products (name, image_name, price, stock, category)
   VALUES
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
   ```

3. **discount_codes**  
   ```sql
   CREATE TABLE discount_codes (
       id INT AUTO_INCREMENT PRIMARY KEY,
       code VARCHAR(50) NOT NULL,
       discount_percent DOUBLE NOT NULL, -- 0.0 to 1.0
       usage_count INT DEFAULT 0,
       active BOOLEAN DEFAULT TRUE
   );
   ```
   > Insert some sample codes:
   ```sql
   INSERT INTO discount_codes (code, discount_percent, active)
   VALUES
     ('WELCOME10', 0.10, TRUE),
     ('50OFF', 0.50, FALSE); -- For example, this might be inactive
   ```

4. **invoices**  
   ```sql
   CREATE TABLE invoices (
       id INT AUTO_INCREMENT PRIMARY KEY,
       order_id VARCHAR(20) NOT NULL,     -- for a unique reference
       date_time DATETIME NOT NULL,
       total DOUBLE NOT NULL
   );
   ```

5. **invoice_items**  
   ```sql
   CREATE TABLE invoice_items (
       id INT AUTO_INCREMENT PRIMARY KEY,
       invoice_id INT NOT NULL,
       product_name VARCHAR(100) NOT NULL, -- stored product name for reference
       quantity INT NOT NULL,
       line_price DOUBLE NOT NULL,
       FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
   );
   ```

Make sure the code in `CoffeeOrderApp` references the correct MySQL connection details:

```java
String url = "jdbc:mysql://localhost:3306/CoffeeOrderDB?useSSL=false&serverTimezone=UTC";
String user = "root";
String password = "root";
connection = DriverManager.getConnection(url, user, password);
```

Adapt `url`, `user`, and `password` to match your local environment.

---

## 5. Directory & Resource Structure

```
📦 CoffeeOrderApp/
  ├── 📂 build/
  │    └── 📂 classes/
  │         └── 📂 App/
  │              ├── 🖼️ images/
  │              │    ├── 🖼️ espresso.png
  │              │    ├── 🖼️ cappuccino.png
  │              │    ├── 🖼️ cheesecake.png
  │              │    ├── 🖼️ americano.png
  │              │    ├── 🖼️ flatwhite.png
  │              │    ├── 🖼️ latte.png
  │              │    ├── 🖼️ matcha.png
  │              │    ├── 🖼️ v60.png
  │              │    └── 🖼️ default.png
  │              └── 🎨 coffee_style.css
  ├── 📂 src/
  │    ├── 📄 AppUser.java
  │    ├── 📄 Category.java
  │    ├── 📄 CoffeeOrderApp.java
  │    ├── 📄 CompletedInvoice.java
  │    ├── 📄 DailySale.java
  │    ├── 📄 DiscountCode.java
  │    ├── 📄 LowStockItem.java
  │    ├── 📄 Product.java
  │    ├── 📄 Role.java
  │    ├── 📄 TopProduct.java
  │    └── 📄 README.md
  │
  └── 📄 README.md
```

In the code, we use a constant path for images:
```java
private static final String IMAGES_DIR = "build/classes/App/images";
```
You may need to modify or ensure this directory is created at runtime. The code attempts to create it if it doesn’t exist.

---

## 6. How to Run

### Option A: Run from an IDE (e.g., IntelliJ, Eclipse, NetBeans)

1. **Clone or copy** the project into your IDE.
2. **Add JavaFX** libraries to the project (if your JDK does not bundle JavaFX).
3. **Adjust DB credentials** in `initDatabase()` if needed.
4. **Run** the `CoffeeOrderApp.java` main method.

### Option B: Using the Command Line (Java 11+)

1. Install JavaFX. 
   - On Windows or macOS, you might download it from [Gluon](https://gluonhq.com/products/javafx/).
   - Unzip the JavaFX SDK, and note the lib folder path.

2. Compile the application (assuming your system has `javac`, `java` on PATH):
   ```bash
   javac --module-path "<PATH_TO_FX_LIB>" --add-modules javafx.controls,javafx.fxml src/App/CoffeeOrderApp.java
   ```
3. Run the compiled application:
   ```bash
   java --module-path "<PATH_TO_FX_LIB>" --add-modules javafx.controls,javafx.fxml App.CoffeeOrderApp
   ```

---

## 7. Usage

1. **Login** with credentials set in your `users` table.  
   - For instance, `username: admin`, `password: admin123` => Admin user.  
   - `username: cashier`, `password: cashier123` => Cashier user.

2. **Admin Role**  
   - See admin sidebar on the left: Inventory, Manage Discounts, Reports.
   - **Inventory**: Add, edit, or delete products. You can drag-drop an image for a product or browse to select it.
   - **Discount Codes**: Manage discount codes (create, edit, delete).
   - **Reports**: 
     - *Daily Sales*: Summaries of total sales grouped by date.  
     - *Top Products*: A bar chart of the top 10 selling products by total quantity sold.  
     - *Low-Stock Items*: A list of items with stock < 5.

3. **Cashier Role**  
   - See cashier sidebar with only a “Discount Code” button.
   - **Orders**: Browse products, filter by category (All, Drinks, Dessert), or search by name/price.  
   - Adjust quantity with `+ / -` buttons.  
   - For drinks, choose Cold or Hot (this adds a label to the invoice item).  
   - Invoice on the right accumulates items.  
   - **Apply Discount Code** if a customer has one.  
   - **Complete Order** => Saves the invoice to DB. Optionally print the receipt.
   - **Cancel** => Clears the invoice (but does not revert product stock changes since the code updates stock upon item addition—this is a design decision you might want to alter if you need fully transactional updates).

---

## 8. Code Overview

- **`CoffeeOrderApp`**: Main class extending `Application`.  
  - **Data Models**: `Product`, `DiscountCode`, `CompletedInvoice`, `AppUser`, and specialized classes for reporting (e.g., `DailySale`, `TopProduct`, `LowStockItem`).  
  - **start(Stage primaryStage)**: Displays the login screen.  
  - **showLoginScreen**, **validateLoginFromDB**: Auth functionality.  
  - **showOrderScene**: Main UI for placing orders, shown after login.  
  - **showAllInvoicesScene**: Lists past invoices.  
  - **showManageDiscountCodesScene**: Admin’s discount code management.  
  - **showInventoryManagementScene** & **showAddEditProductForm**: Admin’s product management.  
  - **showReportsScene**: Admin’s reporting screen with daily sales, top products, and low stock queries.  

- **Styling**: The code references a CSS file (`coffee_style.css`) for UI styling. You can customize it.

---

## 9. Known Considerations & Tips

- **Stock Deduction**: The stock is updated *as soon as an item is added to the invoice*. If the user cancels the order, the stock remains decreased. If you prefer a different behavior, you could do the stock update only after completing the order.
- **Discount Calculation**: The discount percentage is not stored on the invoice or invoice items in the current logic; only the final total is saved. If you need a record of which discount code was applied, you could add a column to the `invoices` table.
- **Error Handling**: A global uncaught exception handler is set to show error alerts. More robust logging or error management might be desired for production use.
- **Image Handling**: The images are copied to `build/classes/App/images/` for usage. This may differ if you package your app as a JAR or deploy it differently. Adjust paths as needed.

---

## 10. Future Improvements

- **Transaction Handling**: For robust systems, each order creation plus stock updates should be atomic using DB transactions.
- **Validation & Edge Cases**: Additional checks for invalid stock, negative prices, etc.
- **Security**: Password hashing, user roles with stricter checks, and improved input validation.
- **UI Polishing**: Possibly rework the layout with responsive design for different screen sizes.

---

## 11. License

This project’s license is not specified in the original code. You can add your own license statement here (e.g., MIT, Apache 2.0, or GPL).

---

**Thank You!**  
Feel free to modify or extend this CoffeeOrderApp for your own use cases or academic projects. If you have any questions or need clarifications, please refer to the code comments or contact the developer. Happy coding!
