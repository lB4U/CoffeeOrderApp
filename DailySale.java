package App;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Date;

public class DailySale {
    private final SimpleObjectProperty<Date> date = new SimpleObjectProperty<>();
    private final SimpleDoubleProperty total = new SimpleDoubleProperty();

    public DailySale(Date date, double total) {
        this.date.set(date);
        this.total.set(total);
    }

    public Date getDate() {
        return date.get();
    }

    public SimpleObjectProperty<Date> dateProperty() {
        return date;
    }

    public double getTotal() {
        return total.get();
    }

    public SimpleDoubleProperty totalProperty() {
        return total;
    }
}
