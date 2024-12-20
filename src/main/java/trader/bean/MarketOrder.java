package trader.bean;

import core.bean.Order;
import core.bean.Side;

public class MarketOrder extends Order {
    public MarketOrder(int orderId, int quantity, double price, Side side, double slipPointLimit) {
        super(orderId, quantity, price, side);
        this.slipPointLimit = slipPointLimit;
    }

    //no consider slip point
    public double slipPointLimit;
}
