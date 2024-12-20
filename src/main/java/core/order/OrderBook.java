package core.order;

import core.EngineConstants;
import core.bean.Order;
import core.bean.Trade;
import core.common.NumberUtil;
import core.price.PriceLevel;
import core.bean.Side;
import core.price.PriceLevelComparator;
import it.unimi.dsi.fastutil.doubles.Double2IntRBTreeMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class OrderBook implements EngineConstants {
    int index = -1;
    final Side side;
    final PriceLevel[] priceLevels = new PriceLevel[MAX_PRICE_LEVELS];
    final Double2IntRBTreeMap priceLevelIndexMap; //price -> {priceIndex}
    OrderIndex ORDER_INDEX = new OrderIndex();

    public OrderBook(Side side) {
        this.side = side;
        this.priceLevelIndexMap = new Double2IntRBTreeMap(new PriceLevelComparator(side));
        initPriceLevels();
    }

    public List<Trade> matchOrder(Order order) {
        if(side.equals(order.getSide())) {
            return Collections.emptyList();
        }

        final List<Trade> trades = new ArrayList<>();
        do {
            final int priceIndex = priceLevelIndexMap.getOrDefault(order.getPrice(), -1);
            if (priceIndex == -1) {
                break;
            }
            final PriceLevel priceLevel = priceLevels[index];

            priceLevel.matchOrder(order, trades);
            if (priceLevel.isEmpty()) {
                priceLevelIndexMap.remove(order.getPrice());
            }
            if (order.isCompleted()) {
                return trades;
            }
        } while (!priceLevelIndexMap.isEmpty());

        return trades;
    }

    public void onCancelOrder(int orderId) {
        OrderIndex.Index idx = ORDER_INDEX.getIndex(orderId);
        PriceLevel priceLevel = priceLevels[idx.priceIndex];
        priceLevel.cancelOrder(idx.priceIndex);
        ORDER_INDEX.removeIndex(orderId);
    }

    public void onNewOrder(Order order) {
        final double price = order.getPrice();
        final int priceIndex = priceLevelIndexMap.getOrDefault(price, -1);
        if (priceIndex != -1) {
            var newIdx = newOrder(order, priceIndex);
            ORDER_INDEX.addIndex(order.getOrderId(), newIdx);
        } else {
            boolean isFound = false;
            int calcCount = MAX_PRICE_LEVELS - 1;
            do{
                calcCount--;
                final int idx = NumberUtil.moduloPowerOfTwo(index + 1, MAX_PRICE_LEVELS);
                if (priceLevels[idx].isEmpty()) {
                    priceLevelIndexMap.put(price, idx);
                    var newIdx = newOrder(order, idx);
                    ORDER_INDEX.addIndex(order.getOrderId(), newIdx);
                    isFound = true;
                    index = idx;
                    break;
                }
            } while (calcCount >= 0);

            if (!isFound) {
                throw new RuntimeException("Price Level Full");
            }
        }
    }

    private OrderIndex.Index newOrder(Order order, int priceLevelIdx) {
        final PriceLevel priceLevel = priceLevels[priceLevelIdx];
        final int oIdx = priceLevel.newOrder(order);
        return new OrderIndex.Index(priceLevelIdx, oIdx);
    }

    private void initPriceLevels() {
        for (int i = 0; i < MAX_PRICE_LEVELS; i++) {
            priceLevels[i] = new PriceLevel(side);
        }
    }

    public OrderIndex getOrderIndex() {
        return ORDER_INDEX;
    }

    public PriceLevel[] getPriceLevels() {
        return priceLevels;
    }
}
