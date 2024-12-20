package core.bean;

public class Order {
    private final Side side;
    private final int orderId;
    protected int quantity;
    private final double price;
    private boolean isCompleted;
    protected int completedQuantity;


    public Order(int orderId, int quantity, double price, Side side) {
        this.orderId = orderId;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.isCompleted = false;
        this.completedQuantity = 0;
    }

    public int getPendingMatchQuantity() {
        return quantity - completedQuantity;
    }

    public double getPrice() {
        return price;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public Side getSide() {
        return side;
    }

    public int getOrderId() {
        return orderId;
    }

    public void makeMatched(int quantity) {
        this.completedQuantity += quantity;
        if (this.completedQuantity > this.quantity) {
            throw new IllegalArgumentException("Value exceeds allocation");
        }
        if (this.quantity == this.completedQuantity) {
            this.isCompleted = true;
        }
    }
}
