package App;

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CoffeeOrderApp extends Application {

    // -------------------------------------------------
    // 1. Enums and Data Models
    // -------------------------------------------------
    // -------------------------------------------------
    // 2. Fields and Logger
    // -------------------------------------------------
    private static final Logger LOGGER = Logger.getLogger(CoffeeOrderApp.class.getName());

    private Role currentUserRole;

    private TextField searchField;
    private TextField minPriceField;
    private TextField maxPriceField;
    private GridPane productGrid;
    private ListView<String> invoiceList;
    private Label totalProductsLabel;
    private Label totalPriceLabel;
    private int totalProducts = 0;
    private double totalPrice = 0.0;
    private Category currentCategory = null;
    private List<Product> products = new ArrayList<>();
    private List<DiscountCode> discountCodesFromDB = new ArrayList<>();
    private double activeDiscountPercent = 0.0;
    private TableView<Product> inventoryTable;

    private Connection connection;
    private static final String IMAGES_DIR = "build/classes/App/images";

    // -------------------------------------------------
    // 3. Application Start and Global Exception Handler
    // -------------------------------------------------
    @Override
    public void start(Stage primaryStage) {
        showLoginScreen(primaryStage);
    }

    public static void main(String[] args) {
        // Set a global uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.SEVERE, "Uncaught Exception in thread " + thread.getName(), throwable);
            // Optionally, show an error alert:
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Unexpected Error");
            alert.setHeaderText(null);
            alert.setContentText("An unexpected error occurred: " + throwable.getMessage());
            alert.showAndWait();
        });
        launch(args);
    }

    // -------------------------------------------------
    // 4. Utility Methods for Alerts and Error Logging
    // -------------------------------------------------
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showError(String title, String msg, Exception ex) {
        LOGGER.log(Level.SEVERE, msg, ex);
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // -------------------------------------------------
    // 5. Login Screen
    // -------------------------------------------------
    private void showLoginScreen(Stage stage) {
        VBox loginLayout = new VBox(10);
        loginLayout.getStyleClass().add("login-layout");
        loginLayout.setPadding(new Insets(20));
        loginLayout.setAlignment(Pos.CENTER);

        Label title = new Label("Coffee Order App - Login");
        title.getStyleClass().add("title-label");

        Label userLabel = new Label("Username:");
        TextField userField = new TextField();

        Label passLabel = new Label("Password:");
        PasswordField passField = new PasswordField();

        Label msgLabel = new Label();
        msgLabel.getStyleClass().add("message-label");

        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().add("action-button");
        loginBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = passField.getText().trim();

            Role loginResult = validateLoginFromDB(user, pass);
            if (loginResult != null) {
                currentUserRole = loginResult;
                if (connection == null) {
                    initDatabase();
                    createImagesDirIfNeeded();
                }
                loadProductsFromDB();
                loadDiscountCodesFromDB();
                showOrderScene(stage);
            } else {
                msgLabel.setText("Invalid credentials!");
            }
        });

        loginLayout.getChildren().addAll(
                title, userLabel, userField, passLabel, passField, loginBtn, msgLabel);

        Scene scene = new Scene(loginLayout, 320, 250);
        scene.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());
        stage.setTitle("Login");
        stage.setScene(scene);
        stage.show();
    }

    private Role validateLoginFromDB(String username, String password) {
        initDatabase();
        String sql = "SELECT password, role FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String dbPass = rs.getString("password");
                String dbRole = rs.getString("role");
                if (dbPass.equals(password)) {
                    return Role.valueOf(dbRole);
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Error validating login.", e);
        }
        return null;
    }

    // -------------------------------------------------
    // 6. Database Initialization
    // -------------------------------------------------
    private void initDatabase() {
        if (connection != null)
            return;
        try {
            // Adjust to your actual DB credentials
            String url = "jdbc:mysql://localhost:3306/CoffeeOrderDB?useSSL=false&serverTimezone=UTC";
            String user = "root";
            String password = "root";
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            showError("Database Error", "Cannot connect to the database.\n" + e.getMessage(), e);
        }
    }

    private void createImagesDirIfNeeded() {
        File imagesDir = new File(IMAGES_DIR);
        if (!imagesDir.exists()) {
            boolean created = imagesDir.mkdirs();
            if (!created) {
                LOGGER.warning("Failed to create images directory: " + IMAGES_DIR);
            }
        }
    }

    // -------------------------------------------------
    // 7. Load Products & Discount Codes from DB
    // -------------------------------------------------
    private void loadProductsFromDB() {
        products.clear();
        if (connection == null)
            return;

        String sql = "SELECT id, name, image_name, price, stock, category FROM products";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String imageName = rs.getString("image_name");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock");
                String catStr = rs.getString("category");
                Category category = catStr.equalsIgnoreCase("DRINK") ? Category.DRINK : Category.DESSERT;

                Product p = new Product(id, name, imageName, price, stock, category);
                products.add(p);
            }

        } catch (SQLException e) {
            showError("DB Error", "Failed to load products: " + e.getMessage(), e);
        }
    }

    private void loadDiscountCodesFromDB() {
        discountCodesFromDB.clear();
        if (connection == null)
            return;

        String sql = "SELECT id, code, discount_percent, usage_count, active FROM discount_codes";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String code = rs.getString("code");
                double percent = rs.getDouble("discount_percent");
                int usage = rs.getInt("usage_count");
                boolean active = rs.getBoolean("active");
                DiscountCode d = new DiscountCode(id, code, percent, usage, active);
                discountCodesFromDB.add(d);
            }
        } catch (SQLException e) {
            showError("DB Error", "Failed to load discount codes.", e);
        }
    }

    // -------------------------------------------------
    // 8. Main Order Scene
    // -------------------------------------------------
    private void showOrderScene(Stage stage) {
        BorderPane orderLayout = new BorderPane();

        // Left sidebar
        if (currentUserRole == Role.ADMIN) {
            orderLayout.setLeft(buildAdminSidebar(stage));
        } else {
            orderLayout.setLeft(buildCashierSidebar());
        }

        // Top nav with advanced search
        HBox topNav = new HBox(10);
        topNav.getStyleClass().add("top-nav");
        topNav.setPadding(new Insets(10));

        Button allBtn = new Button("All");
        allBtn.getStyleClass().add("nav-button");
        allBtn.setOnAction(e -> {
            currentCategory = null;
            refreshProductGrid();
        });

        Button drinksBtn = new Button("Drinks");
        drinksBtn.getStyleClass().add("nav-button");
        drinksBtn.setOnAction(e -> {
            currentCategory = Category.DRINK;
            refreshProductGrid();
        });

        Button dessertBtn = new Button("Dessert");
        dessertBtn.getStyleClass().add("nav-button");
        dessertBtn.setOnAction(e -> {
            currentCategory = Category.DESSERT;
            refreshProductGrid();
        });

        Button invoicesBtn = new Button("Invoices");
        invoicesBtn.getStyleClass().add("nav-button");
        invoicesBtn.setOnAction(e -> showAllInvoicesScene(stage));

        searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.getStyleClass().add("text-field");
        searchField.setOnAction(e -> refreshProductGrid());

        minPriceField = new TextField();
        minPriceField.setPromptText("Min Price");
        minPriceField.setPrefWidth(80);

        maxPriceField = new TextField();
        maxPriceField.setPromptText("Max Price");
        maxPriceField.setPrefWidth(80);

        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("nav-button");
        searchButton.setOnAction(e -> refreshProductGrid());

        topNav.getChildren().addAll(
                allBtn, drinksBtn, dessertBtn, invoicesBtn,
                new Label("Name: "), searchField,
                new Label("Price: "), minPriceField, new Label("-"), maxPriceField,
                searchButton);

        orderLayout.setTop(topNav);

        // Center: product grid
        productGrid = new GridPane();
        productGrid.getStyleClass().add("product-grid");
        productGrid.setHgap(15);
        productGrid.setVgap(15);
        productGrid.setPadding(new Insets(10));
        refreshProductGrid();

        ScrollPane productScroll = new ScrollPane(productGrid);
        productScroll.getStyleClass().add("scroll-pane");
        productScroll.setFitToWidth(true);
        productScroll.setFitToHeight(true);
        orderLayout.setCenter(productScroll);

        // Right: Invoice area
        VBox invoiceSection = new VBox(10);
        invoiceSection.getStyleClass().add("invoice-section");
        invoiceSection.setPadding(new Insets(10));

        Label invoiceTitle = new Label("The Invoice");
        invoiceTitle.getStyleClass().add("invoice-title");

        invoiceList = new ListView<>();
        // Use a custom cell factory to add a style class for invoice items.
        invoiceList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().add("invoice-item");
                }
            }
        });

        HBox totalInfo = new HBox(10);
        totalProductsLabel = new Label("#Products: 0");
        totalPriceLabel = new Label("Total Price: $0.00");
        totalInfo.getChildren().addAll(totalProductsLabel, totalPriceLabel);

        HBox actionButtons = new HBox(10);
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("action-button");
        cancelBtn.setOnAction(e -> resetOrder());

        Button completeBtn = new Button("Complete Order");
        completeBtn.getStyleClass().add("action-button");
        completeBtn.setOnAction(e -> completeOrder());

        actionButtons.getChildren().addAll(cancelBtn, completeBtn);

        invoiceSection.getChildren().addAll(invoiceTitle, invoiceList, totalInfo, actionButtons);
        orderLayout.setRight(invoiceSection);

        Scene orderScene = new Scene(orderLayout, 1100, 700);
        orderScene.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());

        stage.setScene(orderScene);
        stage.setTitle("Coffee Order App (" + currentUserRole + ")");
        stage.show();
    }

    private void refreshProductGrid() {
        productGrid.getChildren().clear();

        String searchText = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase().trim();
        double minPrice = 0.0;
        double maxPrice = Double.MAX_VALUE;

        try {
            minPrice = Double.parseDouble(minPriceField.getText().trim());
        } catch (Exception ex) {
            // ignore
        }
        try {
            maxPrice = Double.parseDouble(maxPriceField.getText().trim());
        } catch (Exception ex) {
            // ignore
        }

        List<Product> filtered = new ArrayList<>();
        for (Product p : products) {
            boolean catMatch = (currentCategory == null) || (p.getCategory() == currentCategory);
            boolean nameMatch = searchText.isEmpty() || p.getName().toLowerCase().contains(searchText);
            boolean priceMatch = (p.getPrice() >= minPrice && p.getPrice() <= maxPrice);

            if (catMatch && nameMatch && priceMatch) {
                filtered.add(p);
            }
        }

        int col = 0;
        int row = 0;
        for (Product p : filtered) {
            VBox box = createProductBox(p);
            productGrid.add(box, col, row);
            col++;
            if (col == 3) {
                col = 0;
                row++;
            }
        }
    }

    // -------------------------------------------------
    // 9. Sidebars (Admin / Cashier)
    // -------------------------------------------------
    private VBox buildAdminSidebar(Stage stage) {
        VBox sidebar = new VBox(15);
        sidebar.getStyleClass().add("admin-sidebar");
        sidebar.setPadding(new Insets(10));

        Button inventoryBtn = new Button("Inventory");
        inventoryBtn.getStyleClass().add("sidebar-button");
        inventoryBtn.setOnAction(e -> showInventoryManagementScene(stage));

        Button discountCodesBtn = new Button("Manage Discounts");
        discountCodesBtn.getStyleClass().add("sidebar-button");
        discountCodesBtn.setOnAction(e -> showManageDiscountCodesScene(stage));

        Button reportsBtn = new Button("Reports");
        reportsBtn.getStyleClass().add("sidebar-button");
        reportsBtn.setOnAction(e -> showReportsScene(stage));

        sidebar.getChildren().addAll(inventoryBtn, discountCodesBtn, reportsBtn);
        return sidebar;
    }

    private VBox buildCashierSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.getStyleClass().add("cashier-sidebar");
        sidebar.setPadding(new Insets(10));

        Button discountCodeBtn = new Button("Discount Code");
        discountCodeBtn.getStyleClass().add("sidebar-button");
        discountCodeBtn.setOnAction(e -> showDiscountCodePopup());

        sidebar.getChildren().add(discountCodeBtn);
        return sidebar;
    }

    // -------------------------------------------------
    // 10. Product Box UI
    // -------------------------------------------------
    private VBox createProductBox(Product product) {
        VBox box = new VBox(5);
        box.getStyleClass().add("product-box");
        box.setPadding(new Insets(5));

        // Attempt to load the product image
        try {
            ImageView iv;
            if (product.getImageName() != null && !product.getImageName().isEmpty()) {
                String imagePath = IMAGES_DIR + File.separator + product.getImageName();
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Image img = new Image(imageFile.toURI().toString());
                    iv = new ImageView(img);
                } else {
                    Image img = new Image(getClass().getResourceAsStream("/App/images/default.png"));
                    iv = new ImageView(img);
                }
            } else {
                Image img = new Image(getClass().getResourceAsStream("/App/images/default.png"));
                iv = new ImageView(img);
            }
            iv.setFitWidth(80);
            iv.setFitHeight(80);
            iv.setPreserveRatio(true);
            box.getChildren().add(iv);
        } catch (Exception e) {
            box.getChildren().add(new Label("[No Image]"));
        }

        Label nameLbl = new Label(product.getName());
        nameLbl.getStyleClass().add("product-name");

        HBox qtyBox = new HBox(5);
        qtyBox.setAlignment(Pos.CENTER_LEFT);
        qtyBox.getStyleClass().add("quantity-box");

        Button minusBtn = new Button("-");
        minusBtn.getStyleClass().add("quantity-button");
        Label qtyLbl = new Label("1");
        qtyLbl.getStyleClass().add("quantity-label");
        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("quantity-button");

        final int[] quantity = { 1 };
        minusBtn.setOnAction(e -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                qtyLbl.setText(String.valueOf(quantity[0]));
            }
        });
        plusBtn.setOnAction(e -> {
            if (quantity[0] < product.getStock()) {
                quantity[0]++;
                qtyLbl.setText(String.valueOf(quantity[0]));
            }
        });

        qtyBox.getChildren().addAll(minusBtn, qtyLbl, plusBtn);

        Label priceLbl = new Label(String.format("$%.2f", product.getPrice()));
        priceLbl.getStyleClass().add("product-price");

        box.getChildren().addAll(nameLbl, priceLbl, qtyBox);

        if (product.getCategory() == Category.DRINK) {
            Button coldBtn = new Button("Cold");
            coldBtn.getStyleClass().add("nav-button");
            coldBtn.setOnAction(e -> {
                if (product.getStock() >= quantity[0]) {
                    double linePrice = product.getPrice() * quantity[0];
                    addToInvoice(product, product.getName() + " (Cold)", quantity[0], linePrice);
                } else {
                    showAlert("Out of Stock", "Not enough stock for " + product.getName());
                }
            });

            Button hotBtn = new Button("Hot");
            hotBtn.getStyleClass().add("nav-button");
            hotBtn.setOnAction(e -> {
                if (product.getStock() >= quantity[0]) {
                    double linePrice = product.getPrice() * quantity[0];
                    addToInvoice(product, product.getName() + " (Hot)", quantity[0], linePrice);
                } else {
                    showAlert("Out of Stock", "Not enough stock for " + product.getName());
                }
            });

            HBox drinkOpts = new HBox(5, coldBtn, hotBtn);
            box.getChildren().add(drinkOpts);

        } else {
            Button addBtn = new Button("Add");
            addBtn.getStyleClass().add("nav-button");
            addBtn.setOnAction(e -> {
                if (product.getStock() >= quantity[0]) {
                    double linePrice = product.getPrice() * quantity[0];
                    addToInvoice(product, product.getName(), quantity[0], linePrice);
                } else {
                    showAlert("Out of Stock", "Not enough stock for " + product.getName());
                }
            });
            box.getChildren().add(addBtn);
        }

        return box;
    }

    private void addToInvoice(Product product, String displayName, int quantity, double linePrice) {
        invoiceList.getItems().add(
                String.format("%s x%d - $%.2f", displayName, quantity, linePrice));
        totalProducts += quantity;
        totalPrice += linePrice;
        updateTotals();

        // Update stock in DB and local product list
        product.setStock(product.getStock() - quantity);
        updateProductStockInDB(product.getId(), product.getStock());
    }

    private void updateProductStockInDB(int productId, int newStock) {
        if (connection == null)
            return;
        String sql = "UPDATE products SET stock = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, newStock);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showError("DB Error", "Failed to update stock: " + e.getMessage(), e);
        }
    }

    private void updateTotals() {
        totalProductsLabel.setText("#Products: " + totalProducts);
        recalculateInvoiceTotalWithPromo();
    }

    private void recalculateInvoiceTotalWithPromo() {
        double discountedTotal = totalPrice * (1.0 - activeDiscountPercent);
        totalPriceLabel.setText(String.format("Total Price: $%.2f", discountedTotal));
    }

    private void resetOrder() {
        invoiceList.getItems().clear();
        totalProducts = 0;
        totalPrice = 0.0;
        activeDiscountPercent = 0.0;
        updateTotals();
    }

    // -------------------------------------------------
    // 11. Completing Orders + Receipt Printing
    // -------------------------------------------------
    private void completeOrder() {
        if (invoiceList.getItems().isEmpty()) {
            showAlert("No Items", "Your invoice is empty!");
            return;
        }
        String orderId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        List<String> lines = new ArrayList<>(invoiceList.getItems());
        double invoiceTotal = totalPrice * (1.0 - activeDiscountPercent);

        int invoiceId = saveInvoiceToDB(orderId, invoiceTotal);
        if (invoiceId > 0) {
            for (String line : lines) {
                String[] parts = line.split(" - ");
                if (parts.length == 2) {
                    String leftPart = parts[0];
                    String rightPart = parts[1];

                    double linePrice = 0.0;
                    try {
                        linePrice = Double.parseDouble(rightPart.replace("$", ""));
                    } catch (Exception ex) {
                        linePrice = 0.0;
                    }

                    int xIndex = leftPart.lastIndexOf('x');
                    if (xIndex > 0) {
                        String productName = leftPart.substring(0, xIndex).trim();
                        String qtyStr = leftPart.substring(xIndex + 1).trim();
                        int quantity = 1;
                        try {
                            quantity = Integer.parseInt(qtyStr);
                        } catch (Exception ex) {
                            quantity = 1;
                        }
                        saveInvoiceItemToDB(invoiceId, productName, quantity, linePrice);
                    }
                }
            }
        }

        Alert a = new Alert(AlertType.INFORMATION);
        a.setTitle("Order Completed");
        a.setHeaderText("Order ID: " + orderId);
        a.setContentText("Date: " + dateTime + "\nTotal: $" + String.format("%.2f", invoiceTotal));
        ButtonType printBtn = new ButtonType("Print Receipt", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeBtn = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(printBtn, closeBtn);

        Optional<ButtonType> result = a.showAndWait();
        if (result.isPresent() && result.get() == printBtn) {
            printReceipt(orderId, dateTime, lines, invoiceTotal);
        }

        resetOrder();
    }

    private int saveInvoiceToDB(String orderId, double total) {
        if (connection == null)
            return -1;
        String sql = "INSERT INTO invoices (order_id, date_time, total) VALUES (?, NOW(), ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, orderId);
            pstmt.setDouble(2, total);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            showError("DB Error", "Failed to save invoice: " + e.getMessage(), e);
        }
        return -1;
    }

    private void saveInvoiceItemToDB(int invoiceId, String productName, int quantity, double linePrice) {
        if (connection == null)
            return;
        String sql = "INSERT INTO invoice_items (invoice_id, product_name, quantity, line_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, invoiceId);
            pstmt.setString(2, productName);
            pstmt.setInt(3, quantity);
            pstmt.setDouble(4, linePrice);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showError("DB Error", "Failed to save invoice item: " + e.getMessage(), e);
        }
    }

    private void printReceipt(String orderId, String dateTime, List<String> lines, double total) {
        StringBuilder sb = new StringBuilder();
        sb.append("**** Coffee Order Receipt ****\n");
        sb.append("Order ID: ").append(orderId).append("\n");
        sb.append("Date/Time: ").append(dateTime).append("\n");
        sb.append("Items:\n");
        for (String line : lines) {
            sb.append("  ").append(line).append("\n");
        }
        sb.append("\nTotal: $").append(String.format("%.2f", total)).append("\n");
        sb.append("**** Thank you! ****\n");

        TextArea textArea = new TextArea(sb.toString());
        textArea.setWrapText(true);

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(null)) {
            boolean success = job.printPage(textArea);
            if (success) {
                job.endJob();
            }
        }
    }

    // -------------------------------------------------
    // 12. Invoices Scene
    // -------------------------------------------------
    private void showAllInvoicesScene(Stage stage) {
        BorderPane layout = new BorderPane();

        Label header = new Label("All Invoices");
        header.getStyleClass().add("scene-header");

        Button backBtn = new Button("Back to Orders");
        backBtn.getStyleClass().add("action-button");
        backBtn.setOnAction(e -> showOrderScene(stage));

        VBox top = new VBox(10, header, backBtn);
        top.setPadding(new Insets(10));
        layout.setTop(top);

        VBox invoiceContainer = new VBox(10);
        invoiceContainer.setPadding(new Insets(10));

        List<CompletedInvoice> allInvoices = loadAllInvoicesFromDB();
        for (CompletedInvoice inv : allInvoices) {
            VBox invBox = new VBox(5);
            invBox.getStyleClass().add("invoice-box");

            Label line1 = new Label("Order ID: " + inv.orderId + " | Date: " + inv.dateTime);
            line1.getStyleClass().add("invoice-box-header");
            invBox.getChildren().add(line1);

            for (String itemLine : inv.items) {
                Label itemLbl = new Label(itemLine);
                itemLbl.getStyleClass().add("invoice-item");
                invBox.getChildren().add(itemLbl);
            }

            Label totalLbl = new Label("Total: $" + String.format("%.2f", inv.total));
            totalLbl.getStyleClass().add("invoice-total");
            invBox.getChildren().add(totalLbl);

            invoiceContainer.getChildren().add(invBox);
        }

        ScrollPane scrollPane = new ScrollPane(invoiceContainer);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setFitToWidth(true);
        layout.setCenter(scrollPane);

        Scene invoiceScene = new Scene(layout, 1000, 700);
        invoiceScene.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());
        stage.setScene(invoiceScene);
        stage.setTitle("All Invoices");
        stage.show();
    }

    private List<CompletedInvoice> loadAllInvoicesFromDB() {
        List<CompletedInvoice> invoices = new ArrayList<>();
        if (connection == null)
            return invoices;

        String sqlInvoices = "SELECT id, order_id, date_time, total FROM invoices ORDER BY id DESC";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sqlInvoices)) {

            while (rs.next()) {
                int invId = rs.getInt("id");
                String orderId = rs.getString("order_id");
                String dt = rs.getString("date_time");
                double total = rs.getDouble("total");

                List<String> lines = new ArrayList<>();
                String sqlItems = "SELECT product_name, quantity, line_price FROM invoice_items WHERE invoice_id = ?";
                try (PreparedStatement pstmt2 = connection.prepareStatement(sqlItems)) {
                    pstmt2.setInt(1, invId);
                    try (ResultSet rsItems = pstmt2.executeQuery()) {
                        while (rsItems.next()) {
                            String productName = rsItems.getString("product_name");
                            int quantity = rsItems.getInt("quantity");
                            double linePrice = rsItems.getDouble("line_price");
                            lines.add(String.format("%s x%d - $%.2f", productName, quantity, linePrice));
                        }
                    }
                }
                invoices.add(new CompletedInvoice(orderId, dt, lines, total));
            }

        } catch (SQLException e) {
            showError("DB Error", "Failed to load invoices: " + e.getMessage(), e);
        }
        return invoices;
    }

    // -------------------------------------------------
    // 13. Discount Code Popup (Cashier)
    // -------------------------------------------------
    private void showDiscountCodePopup() {
        Stage popupStage = new Stage();
        popupStage.setTitle("Enter Discount Code");

        VBox layout = new VBox(10);
        layout.getStyleClass().add("popup-layout");
        layout.setPadding(new Insets(15));

        Label instruction = new Label("Enter Discount Code:");
        TextField codeField = new TextField();
        codeField.setPromptText("e.g. INFLUENCER10");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("action-button");
        cancelBtn.setOnAction(e -> popupStage.close());

        Button applyBtn = new Button("Apply");
        applyBtn.getStyleClass().add("action-button");
        applyBtn.setOnAction(e -> {
            String enteredCode = codeField.getText().trim();
            DiscountCode found = findDiscountCodeInDB(enteredCode);
            if (found == null || !found.isActive()) {
                errorLabel.setText("Invalid or Inactive Discount Code!");
            } else {
                activeDiscountPercent = found.getDiscountPercent();
                incrementDiscountUsage(found);
                recalculateInvoiceTotalWithPromo();

                Alert success = new Alert(AlertType.INFORMATION);
                success.setTitle("Discount Applied");
                success.setHeaderText(null);
                success.setContentText(String.format(
                        "Discount Applied: %.0f%% off\n(Usage Count: %d)",
                        found.getDiscountPercent() * 100,
                        found.getUsageCount() + 1));
                success.showAndWait();

                popupStage.close();
            }
        });

        buttonBox.getChildren().addAll(cancelBtn, applyBtn);
        layout.getChildren().addAll(instruction, codeField, errorLabel, buttonBox);

        Scene scene = new Scene(layout, 300, 150);
        scene.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    private DiscountCode findDiscountCodeInDB(String code) {
        for (DiscountCode dc : discountCodesFromDB) {
            if (dc.getCode().equalsIgnoreCase(code)) {
                return dc;
            }
        }
        return null;
    }

    private void incrementDiscountUsage(DiscountCode dc) {
        dc.incrementUsage();
        String sql = "UPDATE discount_codes SET usage_count = usage_count + 1 WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, dc.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            showError("DB Error", "Failed to increment discount usage.", e);
        }
        loadDiscountCodesFromDB();
    }

    // -------------------------------------------------
    // 14. Inventory Management (Admin)
    // -------------------------------------------------
    private void showInventoryManagementScene(Stage stage) {
        BorderPane root = new BorderPane();

        Label title = new Label("Inventory Management");
        title.getStyleClass().add("scene-header");

        Button backBtn = new Button("Back to Orders");
        backBtn.getStyleClass().add("action-button");
        backBtn.setOnAction(e -> showOrderScene(stage));

        HBox topBar = new HBox(10, title, backBtn);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(topBar);

        inventoryTable = new TableView<>();
        inventoryTable.getStyleClass().add("inventory-table");

        TableColumn<Product, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);

        TableColumn<Product, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(80);

        TableColumn<Product, Integer> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));
        stockCol.setPrefWidth(80);

        TableColumn<Product, Category> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setPrefWidth(100);

        inventoryTable.getColumns().clear();
        inventoryTable.getColumns().addAll(nameCol, priceCol, stockCol, catCol);

        inventoryTable.getItems().clear();
        inventoryTable.getItems().addAll(products);

        root.setCenter(inventoryTable);

        Button addBtn = new Button("Add Product");
        addBtn.getStyleClass().add("action-button");
        addBtn.setOnAction(e -> showAddEditProductForm(stage, null));

        Button editBtn = new Button("Edit Product");
        editBtn.getStyleClass().add("action-button");
        editBtn.setOnAction(e -> {
            Product selected = inventoryTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAddEditProductForm(stage, selected);
            } else {
                showAlert("No Selection", "Please select a product to edit.");
            }
        });

        Button deleteBtn = new Button("Delete Product");
        deleteBtn.getStyleClass().add("action-button");
        deleteBtn.setOnAction(e -> {
            Product selected = inventoryTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteProductFromDB(selected.getId());
                products.remove(selected);
                inventoryTable.getItems().remove(selected);
            } else {
                showAlert("No Selection", "Please select a product to delete.");
            }
        });

        HBox btnBox = new HBox(10, addBtn, editBtn, deleteBtn);
        btnBox.setPadding(new Insets(10));
        btnBox.setAlignment(Pos.CENTER);

        root.setBottom(btnBox);

        Scene scene = new Scene(root, 800, 500);
        scene.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Inventory Management");
        stage.show();
    }

    private void deleteProductFromDB(int productId) {
        if (connection == null)
            return;
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showError("DB Error", "Failed to delete product: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------
    // 15. Add/Edit Product Form
    // -------------------------------------------------
    private void showAddEditProductForm(Stage stage, Product productToEdit) {
        Stage formStage = new Stage();
        formStage.setTitle(productToEdit == null ? "Add Product" : "Edit Product");

        VBox root = new VBox(10);
        root.getStyleClass().add("form-layout");
        root.setPadding(new Insets(15));

        TextField nameField = new TextField();
        nameField.setPromptText("Product Name");

        TextField priceField = new TextField();
        priceField.setPromptText("Price");

        TextField stockField = new TextField();
        stockField.setPromptText("Stock");

        ComboBox<Category> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(Category.values());

        VBox imageUploadBox = new VBox(5);
        imageUploadBox.getStyleClass().add("drag-and-drop");
        Label uploadLabel = new Label("Drag image here or click to browse");
        uploadLabel.getStyleClass().add("drag-and-drop-label");
        ImageView imagePreview = new ImageView();
        imagePreview.getStyleClass().add("image-preview");
        imagePreview.setFitWidth(150);
        imagePreview.setFitHeight(150);
        imagePreview.setPreserveRatio(true);

        final File[] selectedImageFile = { null };

        imageUploadBox.setOnMouseClicked(ev -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose Product Image");
            File file = fileChooser.showOpenDialog(formStage);
            if (file != null) {
                selectedImageFile[0] = file;
                loadImagePreview(file, imagePreview);
            }
        });
        imageUploadBox.setOnDragOver(ev -> {
            if (ev.getGestureSource() != imageUploadBox && ev.getDragboard().hasFiles()) {
                ev.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            ev.consume();
        });
        imageUploadBox.setOnDragDropped(ev -> {
            var db = ev.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                selectedImageFile[0] = file;
                loadImagePreview(file, imagePreview);
            }
            ev.setDropCompleted(true);
            ev.consume();
        });
        imageUploadBox.getChildren().addAll(uploadLabel, imagePreview);

        if (productToEdit != null) {
            nameField.setText(productToEdit.getName());
            priceField.setText(String.valueOf(productToEdit.getPrice()));
            stockField.setText(String.valueOf(productToEdit.getStock()));
            categoryBox.setValue(productToEdit.getCategory());

            if (productToEdit.getImageName() != null && !productToEdit.getImageName().isEmpty()) {
                try {
                    String existingImagePath = IMAGES_DIR + File.separator + productToEdit.getImageName();
                    File existingFile = new File(existingImagePath);
                    if (existingFile.exists()) {
                        selectedImageFile[0] = existingFile;
                        loadImagePreview(existingFile, imagePreview);
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("action-button");
        saveBtn.setOnAction(e -> {
            boolean valid = true;
            // Simple validation with error styling
            if (nameField.getText().trim().isEmpty()) {
                if (!nameField.getStyleClass().contains("error")) {
                    nameField.getStyleClass().add("error");
                }
                valid = false;
            } else {
                nameField.getStyleClass().remove("error");
            }
            if (priceField.getText().trim().isEmpty()) {
                if (!priceField.getStyleClass().contains("error")) {
                    priceField.getStyleClass().add("error");
                }
                valid = false;
            } else {
                priceField.getStyleClass().remove("error");
            }
            if (stockField.getText().trim().isEmpty()) {
                if (!stockField.getStyleClass().contains("error")) {
                    stockField.getStyleClass().add("error");
                }
                valid = false;
            } else {
                stockField.getStyleClass().remove("error");
            }
            if (categoryBox.getValue() == null) {
                if (!categoryBox.getStyleClass().contains("error")) {
                    categoryBox.getStyleClass().add("error");
                }
                valid = false;
            } else {
                categoryBox.getStyleClass().remove("error");
            }
            if (!valid) {
                showAlert("Invalid Input", "Please fill all fields correctly.");
                return;
            }
            try {
                double price = Double.parseDouble(priceField.getText());
                int stock = Integer.parseInt(stockField.getText());

                String storedImageName;
                if (selectedImageFile[0] != null) {
                    createImagesDirIfNeeded();
                    String destPath = IMAGES_DIR + File.separator + selectedImageFile[0].getName();
                    Files.copy(
                            selectedImageFile[0].toPath(),
                            Paths.get(destPath),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    storedImageName = selectedImageFile[0].getName();
                } else {
                    if (productToEdit != null && productToEdit.getImageName() != null) {
                        storedImageName = productToEdit.getImageName();
                    } else {
                        storedImageName = "default.png";
                    }
                }

                if (productToEdit == null) {
                    Product newProduct = new Product(
                            nameField.getText(),
                            storedImageName,
                            price,
                            stock,
                            categoryBox.getValue());
                    int newId = insertProductIntoDB(newProduct);
                    if (newId > 0) {
                        newProduct.setId(newId);
                        products.add(newProduct);
                        inventoryTable.getItems().add(newProduct);
                    }
                } else {
                    productToEdit.setName(nameField.getText());
                    productToEdit.setPrice(price);
                    productToEdit.setStock(stock);
                    productToEdit.setCategory(categoryBox.getValue());
                    productToEdit.setImageName(storedImageName);

                    updateProductInDB(productToEdit);
                    inventoryTable.refresh();
                }
                formStage.close();

            } catch (NumberFormatException ex1) {
                showAlert("Invalid Number", "Price/Stock must be valid numbers.");
            } catch (IOException ex2) {
                showError("Image Save Error", "Failed to save the image: " + ex2.getMessage(), ex2);
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("action-button");
        cancelBtn.setOnAction(e2 -> formStage.close());

        HBox btnBox = new HBox(10, cancelBtn, saveBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(
                new Label("Name:"), nameField,
                new Label("Price:"), priceField,
                new Label("Stock:"), stockField,
                new Label("Category:"), categoryBox,
                imageUploadBox,
                btnBox);

        Scene scene = new Scene(root, 350, 520);
        scene.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());
        formStage.setScene(scene);
        formStage.show();
    }

    private int insertProductIntoDB(Product product) {
        if (connection == null)
            return -1;
        String sql = "INSERT INTO products (name, image_name, price, stock, category) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getImageName());
            pstmt.setDouble(3, product.getPrice());
            pstmt.setInt(4, product.getStock());
            pstmt.setString(5, product.getCategory().name());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            showError("DB Error", "Failed to insert product: " + e.getMessage(), e);
        }
        return -1;
    }

    private void updateProductInDB(Product product) {
        if (connection == null)
            return;
        String sql = "UPDATE products SET name = ?, image_name = ?, price = ?, stock = ?, category = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getImageName());
            pstmt.setDouble(3, product.getPrice());
            pstmt.setInt(4, product.getStock());
            pstmt.setString(5, product.getCategory().name());
            pstmt.setInt(6, product.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showError("DB Error", "Failed to update product: " + e.getMessage(), e);
        }
    }

    private void loadImagePreview(File file, ImageView imagePreview) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Image img = new Image(fis);
            imagePreview.setImage(img);
        } catch (Exception ex) {
            showError("Image Error", "Could not load image: " + ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------
    // 16. Manage Discount Codes (Admin)
    // -------------------------------------------------
    private void showManageDiscountCodesScene(Stage stage) {
        BorderPane root = new BorderPane();

        Label title = new Label("Manage Discount Codes");
        title.getStyleClass().add("scene-header");

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("action-button");
        backBtn.setOnAction(e -> showOrderScene(stage));

        HBox topBar = new HBox(10, title, backBtn);
        topBar.setPadding(new Insets(10));
        root.setTop(topBar);

        TableView<DiscountCode> discountTable = new TableView<>();
        discountTable.getStyleClass().add("discount-table");
        discountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DiscountCode, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));

        TableColumn<DiscountCode, Double> percentCol = new TableColumn<>("Discount %");
        percentCol.setCellValueFactory(new PropertyValueFactory<>("discountPercent"));

        TableColumn<DiscountCode, Integer> usageCol = new TableColumn<>("Usage Count");
        usageCol.setCellValueFactory(new PropertyValueFactory<>("usageCount"));

        TableColumn<DiscountCode, Boolean> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));

        discountTable.getColumns().addAll(codeCol, percentCol, usageCol, activeCol);
        discountTable.getItems().addAll(discountCodesFromDB);

        root.setCenter(discountTable);

        Button addBtn = new Button("Add Code");
        addBtn.getStyleClass().add("action-button");
        addBtn.setOnAction(e -> showAddEditDiscountCode(null, discountTable));
        Button editBtn = new Button("Edit Code");
        editBtn.getStyleClass().add("action-button");
        editBtn.setOnAction(e -> {
            DiscountCode selected = discountTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAddEditDiscountCode(selected, discountTable);
            }
        });
        Button deleteBtn = new Button("Delete Code");
        deleteBtn.getStyleClass().add("action-button");
        deleteBtn.setOnAction(e -> {
            DiscountCode selected = discountTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteDiscountCodeFromDB(selected.getId());
                loadDiscountCodesFromDB();
                discountTable.getItems().setAll(discountCodesFromDB);
            }
        });

        HBox bottomBar = new HBox(10, addBtn, editBtn, deleteBtn);
        bottomBar.setPadding(new Insets(10));
        bottomBar.setAlignment(Pos.CENTER);

        root.setBottom(bottomBar);

        Scene sc = new Scene(root, 600, 400);
        sc.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());
        stage.setScene(sc);
        stage.setTitle("Manage Discounts");
        stage.show();
    }

    private void showAddEditDiscountCode(DiscountCode dc, TableView<DiscountCode> table) {
        Stage formStage = new Stage();
        formStage.setTitle(dc == null ? "Add Code" : "Edit Code");

        VBox layout = new VBox(10);
        layout.getStyleClass().add("form-layout");
        layout.setPadding(new Insets(15));

        TextField codeField = new TextField();
        codeField.setPromptText("Code");
        TextField percentField = new TextField();
        percentField.setPromptText("Discount % (0.0 to 1.0)");

        CheckBox activeBox = new CheckBox("Active");

        if (dc != null) {
            codeField.setText(dc.getCode());
            percentField.setText(String.valueOf(dc.getDiscountPercent()));
            activeBox.setSelected(dc.isActive());
        }

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("action-button");
        saveBtn.setOnAction(e -> {
            if (codeField.getText().trim().isEmpty() || percentField.getText().trim().isEmpty()) {
                showAlert("Invalid", "Please fill all fields.");
                return;
            }
            try {
                double disc = Double.parseDouble(percentField.getText().trim());

                // ----------------------------
                // Cap the discount at 1.0
                // ----------------------------
                if (disc > 1.0) {
                    disc = 1.0;
                } else if (disc < 0.0) {
                    disc = 0.0;
                }
                // ----------------------------

                if (dc == null) {
                    insertDiscountCodeToDB(codeField.getText().trim(), disc, activeBox.isSelected());
                } else {
                    updateDiscountCodeInDB(dc.getId(), codeField.getText().trim(), disc, activeBox.isSelected());
                }
                loadDiscountCodesFromDB();
                table.getItems().setAll(discountCodesFromDB);
                formStage.close();

            } catch (NumberFormatException ex1) {
                showAlert("Invalid Number", "Discount% must be a number between 0.0 and 1.0.");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("action-button");
        cancelBtn.setOnAction(e2 -> formStage.close());

        layout.getChildren().addAll(new Label("Code:"), codeField,
                new Label("Discount %:"), percentField,
                activeBox,
                new HBox(10, cancelBtn, saveBtn));

        Scene sc = new Scene(layout, 300, 200);
        sc.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());
        formStage.setScene(sc);
        formStage.show();
    }

    private void insertDiscountCodeToDB(String code, double disc, boolean active) {
        if (connection == null)
            return;
        String sql = "INSERT INTO discount_codes (code, discount_percent, active) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setDouble(2, disc);
            ps.setBoolean(3, active);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError("DB Error", "Failed to insert discount code: " + e.getMessage(), e);
        }
    }

    private void updateDiscountCodeInDB(int id, String code, double disc, boolean active) {
        if (connection == null)
            return;
        String sql = "UPDATE discount_codes SET code = ?, discount_percent = ?, active = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setDouble(2, disc);
            ps.setBoolean(3, active);
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError("DB Error", "Failed to update discount code: " + e.getMessage(), e);
        }
    }

    private void deleteDiscountCodeFromDB(int id) {
        if (connection == null)
            return;
        String sql = "DELETE FROM discount_codes WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError("DB Error", "Failed to delete discount code: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------
    // 17. Reports (Sales & Inventory)
    // -------------------------------------------------
    private void showReportsScene(Stage stage) {
        BorderPane root = new BorderPane();

        Label title = new Label("Reports");
        title.getStyleClass().add("scene-header");

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("action-button");
        backBtn.setOnAction(e -> showOrderScene(stage));

        HBox topBar = new HBox(10, title, backBtn);
        topBar.setPadding(new Insets(10));
        root.setTop(topBar);

        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(15));
        mainBox.getStyleClass().add("reports-box");

        Button dailySalesBtn = new Button("Daily Sales");
        dailySalesBtn.getStyleClass().add("report-button");
        dailySalesBtn.setOnAction(e -> showDailySales());

        Button topProductsBtn = new Button("Top-Selling Products");
        topProductsBtn.getStyleClass().add("report-button");
        topProductsBtn.setOnAction(e -> showTopProducts());

        Button lowStockBtn = new Button("Low-Stock Products");
        lowStockBtn.getStyleClass().add("report-button");
        lowStockBtn.setOnAction(e -> showLowStock());

        mainBox.getChildren().addAll(dailySalesBtn, topProductsBtn, lowStockBtn);
        root.setCenter(mainBox);

        Scene sc = new Scene(root, 600, 400);
        sc.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());
        stage.setScene(sc);
        stage.setTitle("Reports");
        stage.show();
    }

    private void showDailySales() {
        if (connection == null) {
            showAlert("DB Error", "Not connected to DB.");
            return;
        }
        List<DailySale> dailySales = new ArrayList<>();
        String sql = "SELECT DATE(date_time) as dt, SUM(total) as daily_total " +
                "FROM invoices GROUP BY DATE(date_time) ORDER BY dt DESC";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date date = rs.getDate("dt");
                double sum = rs.getDouble("daily_total");
                dailySales.add(new DailySale(date, sum));
            }
        } catch (SQLException e) {
            showError("Error", e.getMessage(), e);
            return;
        }

        TableView<DailySale> table = new TableView<>();
        table.getStyleClass().add("report-table");

        TableColumn<DailySale, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate().toString()));
        TableColumn<DailySale, Number> totalCol = new TableColumn<>("Total Sales");
        totalCol.setCellValueFactory(cellData -> cellData.getValue().totalProperty());

        table.getColumns().addAll(dateCol, totalCol);
        table.getItems().setAll(dailySales);

        Stage stage = new Stage();
        stage.setTitle("Daily Sales Report");
        BorderPane pane = new BorderPane(table);
        Scene scene = new Scene(pane, 400, 400);
        scene.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void showTopProducts() {
        if (connection == null) {
            showAlert("DB Error", "Not connected to DB.");
            return;
        }
        List<TopProduct> topProducts = new ArrayList<>();
        String sql = "SELECT product_name, SUM(quantity) as total_sold " +
                "FROM invoice_items GROUP BY product_name " +
                "ORDER BY total_sold DESC LIMIT 10";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("product_name");
                int sold = rs.getInt("total_sold");
                topProducts.add(new TopProduct(name, sold));
            }
        } catch (SQLException e) {
            showError("DB Error", "Failed to load top products.", e);
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Units Sold");
        xAxis.setLabel("Product");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Top-Selling Products");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Products");

        for (TopProduct tp : topProducts) {
            series.getData().add(new XYChart.Data<>(tp.getName(), tp.getUnitsSold()));
        }
        barChart.getData().add(series);

        Stage stage = new Stage();
        stage.setTitle("Top-Selling Products");
        Scene scene = new Scene(barChart, 600, 400);
        scene.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void showLowStock() {
        if (connection == null) {
            showAlert("DB Error", "Not connected to DB.");
            return;
        }
        List<LowStockItem> lowStockItems = new ArrayList<>();
        String sql = "SELECT name, stock FROM products WHERE stock < 5 ORDER BY stock ASC";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("name");
                int stck = rs.getInt("stock");
                lowStockItems.add(new LowStockItem(name, stck));
            }
        } catch (SQLException e) {
            showError("DB Error", "Failed to load low-stock products.", e);
            return;
        }

        TableView<LowStockItem> table = new TableView<>();
        table.getStyleClass().add("report-table");

        TableColumn<LowStockItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        TableColumn<LowStockItem, Number> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(cellData -> cellData.getValue().stockProperty());

        table.getColumns().addAll(nameCol, stockCol);
        table.getItems().addAll(lowStockItems);

        Stage stage = new Stage();
        stage.setTitle("Low-Stock Products");
        BorderPane pane = new BorderPane(table);
        Scene scene = new Scene(pane, 400, 400);
        scene.getStylesheets().add(getClass().getResource("/App/coffee_style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
}
