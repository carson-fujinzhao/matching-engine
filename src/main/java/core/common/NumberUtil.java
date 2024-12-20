package core.common;

import core.EngineConstants;

public class NumberUtil implements EngineConstants {
    public static int moduloPowerOfTwo(int v, int d) {
        return v & (d - 1);
    }

    public static int coalesceOrderPriceIndexAndPriceLevelIndex(int priceIndex, int priceLevelIndex) {
        return (priceIndex << 15) | (priceLevelIndex);
    }

    public static int getPriceLevelIndex(int coalescedVal) {
        return coalescedVal & (MAX_ORDERS_AT_EACH_PRICE_LEVEL - 1);
    }

    public static int getPriceIndex(int coalescedVal) {
        return (coalescedVal >> 15) & (MAX_PRICE_LEVELS - 1);
    }
}
