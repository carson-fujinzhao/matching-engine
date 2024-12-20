package core.price;


import core.EngineConstants;
import core.bean.Order;
import core.bean.Side;
import core.bean.Trade;
import core.common.NumberUtil;
import it.unimi.dsi.fastutil.Pair;

import java.util.List;

import static core.common.NumberUtil.moduloPowerOfTwo;

public class PriceLevel implements EngineConstants {
    double price;
    final Side side;
    int[][] orderQuantity = new int[MAX_ORDERS_AT_EACH_PRICE_LEVEL][2]; // {orderId, quantity}
    int start = -1, end = -1;
    int openOrderCount = 0;
    int openQuantity = 0;

    public PriceLevel(Side side) {
        this.side = side;
    }

    public int newOrder(Order order) {
        this.price = order.getPrice();
        int idx = moduloPowerOfTwo(end + 1, MAX_ORDERS_AT_EACH_PRICE_LEVEL);
        if (idx == start) throw new RuntimeException("Price Level Full");
        orderQuantity[idx][0] = order.getOrderId();
        orderQuantity[idx][1] = order.getPendingMatchQuantity();
        end = idx;
        openOrderCount++;
        openQuantity += order.getPendingMatchQuantity();
        return idx;
    }

    public void cancelOrder(int idx) {
        if (orderQuantity[idx][0] == -1) {
            return;
        }

        makeInvalid(idx);

        do {
            start = nextIndex(idx);
        } while (start != -1 && orderQuantity[start][0] == -1 && start < end);
    }

    public boolean isEmpty() {
        return openOrderCount == 0;
    }

    public void makeInvalid(int idx) {
        openQuantity -= orderQuantity[idx][1];
        openOrderCount--;
        orderQuantity[idx][0] = -1;
        orderQuantity[idx][1] = -1;
    }

    public void matchOrder(Order order, List<Trade> trades) {
        if (openOrderCount == 0) {
            return;
        }

        if (start == -1 && end >= 0) {
            start = 0;
        }

        int currentIdx = start;

        while (openOrderCount > 0) {
            int targetOrderId = orderQuantity[currentIdx][0];
            int targetQuantity = orderQuantity[currentIdx][1];

            if (targetOrderId <= 0 || targetQuantity <= 0) {
                currentIdx = nextIndex(currentIdx);
                start = currentIdx;
                continue;
            }

            int tradeQuantity = Math.min(order.getPendingMatchQuantity(), targetQuantity);
            trades.add(new Trade(order.getOrderId(), targetOrderId, price, tradeQuantity));
            orderQuantity[currentIdx][1] -= tradeQuantity;
            openQuantity -= tradeQuantity;

            order.makeMatched(tradeQuantity);
            if (orderQuantity[currentIdx][1] == 0) {
                makeInvalid(currentIdx);
            }

            // if activityOrder completed, stop
            if (order.isCompleted()) {
                break;
            }
            currentIdx = nextIndex(currentIdx);
        }

        start = currentIdx;
    }

    private int nextIndex(int idx) {
        if (idx == end) {
            return -1;
        }
        return NumberUtil.moduloPowerOfTwo(idx + 1, MAX_ORDERS_AT_EACH_PRICE_LEVEL);
    }

    public double getPrice() {
        return price;
    }

    public int getOpenQuantity() {
        return openQuantity;
    }

    public int getOpenOrderCount() {
        return openOrderCount;
    }

    //below for test
    public Pair<Integer, Integer> getStartOrder() {
        return Pair.of(orderQuantity[start][0], orderQuantity[start][1]);
    }

    public Pair<Integer, Integer> getEndOrder() {
        return Pair.of(orderQuantity[end][0], orderQuantity[end][1]);
    }

    public Pair<Integer, Integer> getIndexEndOrder(int idx) {
        return Pair.of(orderQuantity[idx][0], orderQuantity[idx][1]);
    }
}
