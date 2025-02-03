package App;

import java.util.List;

public class CompletedInvoice {
    String orderId;
    String dateTime;
    List<String> items;
    double total;

    public CompletedInvoice(String orderId, String dateTime, List<String> items, double total) {
        this.orderId = orderId;
        this.dateTime = dateTime;
        this.items = items;
        this.total = total;
    }
}
