package trader;

import core.Engine;
import core.bean.Order;
import core.bean.Side;
import core.bean.Trade;
import trader.bean.MarketOrder;

import java.util.ArrayList;
import java.util.List;

public class MarketOrderHandler extends Engine {
    int orderId = 0;
    TradePersistence marketTradePersistence = new TradePersistence();

    public void matchOrder(int quantity, double price, Side side) {
        List<Trade> trades = new ArrayList<>();
        MarketOrder order = new MarketOrder(orderId++, quantity, price, side, 0);
        Engine.OPPOSITE_ORDER_BOOK(side).matchOrder(order);
        marketTradePersistence.addTrades(trades);
    }

    public void cancelOrder(Order order) {
        Engine.ORDER_BOOK(order.getSide()).onCancelOrder(order.getOrderId());
    }
}