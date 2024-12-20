package core;

import core.bean.Side;
import core.order.OrderBook;

public class Engine {
    public static OrderBook SELL_ORDER_BOOK;
    public static OrderBook BUY_ORDER_BOOK;

    public Engine() {
        SELL_ORDER_BOOK = new OrderBook(Side.SELL);
        BUY_ORDER_BOOK = new OrderBook(Side.BUY);
    }

    public static OrderBook OPPOSITE_ORDER_BOOK(Side side) {
        if (side == null) {
            throw new IllegalArgumentException("Unsupported order side");
        }
        return side == Side.SELL ? BUY_ORDER_BOOK : SELL_ORDER_BOOK;
    }

    public static OrderBook ORDER_BOOK(Side side) {
        if (side == null) {
            throw new IllegalArgumentException("Unsupported order side");
        }
        return side == Side.SELL ? SELL_ORDER_BOOK : BUY_ORDER_BOOK;
    }
}
