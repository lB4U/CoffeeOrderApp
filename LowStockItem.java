package App;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class LowStockItem {
    private final SimpleStringProperty name = new SimpleStringProperty();
    private final SimpleIntegerProperty stock = new SimpleIntegerProperty();

    public LowStockItem(String name, int stock) {
        this.name.set(name);
        this.stock.set(stock);
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public int getStock() {
        return stock.get();
    }

    public SimpleIntegerProperty stockProperty() {
        return stock;
    }
}
