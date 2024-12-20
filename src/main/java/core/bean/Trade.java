package core.bean;

public class Trade {
    private final int activeOrderId;
    private final int passiveOrderId;
    private final double price;
    private final int quantity;
    private final long tradeTime;

    public Trade(int activeOrderId, int passiveOrderId, double tradePrice, int tradeQuantity) {
        this.activeOrderId = activeOrderId;
        this.passiveOrderId = passiveOrderId;
        this.price = tradePrice;
        this.quantity = tradeQuantity;
        this.tradeTime = System.currentTimeMillis();
    }

    public int getActiveOrderId() {
        return activeOrderId;
    }

    public int getPassiveOrderId() {
        return passiveOrderId;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTradeTime() {
        return tradeTime;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "activeOrderId=" + activeOrderId +
                ", passiveOrderId=" + passiveOrderId +
                ", tradePrice=" + price +
                ", tradeQuantity=" + quantity +
                ", tradeTime=" + tradeTime +
                '}';
    }
}
