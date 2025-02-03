package App;

public class Product {
    private int id;
    private String name;
    private String imageName;
    private double price;
    private int stock;
    private Category category;

    public Product(int id, String name, String imageName, double price, int stock, Category category) {
        this.id = id;
        this.name = name;
        this.imageName = imageName;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }

    public Product(String name, String imageName, double price, int stock, Category category) {
        this(0, name, imageName, price, stock, category);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
