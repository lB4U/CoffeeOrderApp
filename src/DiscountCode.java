package App;

public class DiscountCode {
    private int id; // primary key in DB
    private String code;
    private double discountPercent;
    private int usageCount;
    private boolean active;

    public DiscountCode(int id, String code, double discountPercent, int usageCount, boolean active) {
        this.id = id;
        this.code = code;
        this.discountPercent = discountPercent;
        this.usageCount = usageCount;
        this.active = active;
    }

    public DiscountCode(String code, double discountPercent) {
        this(0, code, discountPercent, 0, true);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void incrementUsage() {
        this.usageCount++;
    }
}
