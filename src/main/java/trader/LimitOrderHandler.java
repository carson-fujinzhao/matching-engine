package trader;

import core.Engine;
import core.bean.Side;
import core.bean.Trade;
import trader.bean.LimitOrder;

import java.util.ArrayList;
import java.util.List;

public class LimitOrderHandler extends Engine {

    int orderId = 100000000;

    TradePersistence marketTradePersistence = new TradePersistence();

    public List<Trade> matchOrder(int quantity, double price, Side side) {
        List<Trade> trades = new ArrayList<>();
        LimitOrder order = new LimitOrder(orderId++, quantity, price, side);
        trades = Engine.OPPOSITE_ORDER_BOOK(side).matchOrder(order);
        marketTradePersistence.addTrades(trades);

        if (!order.isCompleted()){
            Engine.ORDER_BOOK(side).onNewOrder(order);
        }
        return trades;
    }

    public void cancelOrder(Side side, int orderId) {
        Engine.ORDER_BOOK(side).onCancelOrder(orderId);
    }

}
