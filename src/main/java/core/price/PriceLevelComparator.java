package core.price;

import core.bean.Side;

import java.util.Comparator;

public class PriceLevelComparator implements Comparator<Double> {
    private final Side side;

    public PriceLevelComparator(Side side) {
        this.side = side;
    }

    @Override
    public int compare(Double price1, Double price2) {
        if (side == Side.BUY) {
            return price2.compareTo(price1);
        } else {
            return price1.compareTo(price2);
        }
    }
}
