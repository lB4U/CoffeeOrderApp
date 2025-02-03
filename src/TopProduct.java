package App;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class TopProduct {
    private final SimpleStringProperty name = new SimpleStringProperty();
    private final SimpleIntegerProperty unitsSold = new SimpleIntegerProperty();

    public TopProduct(String name, int unitsSold) {
        this.name.set(name);
        this.unitsSold.set(unitsSold);
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public int getUnitsSold() {
        return unitsSold.get();
    }

    public SimpleIntegerProperty unitsSoldProperty() {
        return unitsSold;
    }
}
