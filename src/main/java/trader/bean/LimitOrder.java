package trader.bean;

import core.bean.Order;
import core.bean.Side;

public class LimitOrder extends Order {
    public LimitOrder(int orderId, int quantity, double price, Side side) {
        super(orderId, quantity, price, side);
    }
}
