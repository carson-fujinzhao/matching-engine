package core.order;

import core.common.NumberUtil;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class OrderIndex {
    final Int2IntMap orderIndex = new Int2IntOpenHashMap(); // orderId -> {priceIndex & priceLevelIndex}

    protected void addIndex(int orderId, Index idx) {
        orderIndex.put(orderId, idx.coalesceValue());
    }

    public Index getIndex(int orderId) {
        final int val = orderIndex.get(orderId);
        return new Index(val);
    }

    protected void removeIndex(int orderId) {
        orderIndex.remove(orderId);
    }

    public static class Index {
        int priceIndex;
        int priceLevelIndex;

        public Index(int priceIndex, int priceLevelIndex) {
            this.priceIndex = priceIndex;
            this.priceLevelIndex = priceLevelIndex;
        }

        public Index(int val) {
            this.priceIndex = NumberUtil.getPriceIndex(val);
            this.priceLevelIndex = NumberUtil.getPriceLevelIndex(val);
        }

        public int coalesceValue() {
            return NumberUtil.coalesceOrderPriceIndexAndPriceLevelIndex(priceIndex, priceLevelIndex);
        }
    }
}
