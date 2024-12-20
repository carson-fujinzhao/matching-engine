package trader;

import core.bean.Trade;

import java.util.ArrayList;
import java.util.List;

public class TradePersistence {
    List<Trade> trades = new ArrayList<>();

    public void addTrades(List<Trade> trade) {
        trades.addAll(trade);
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public int getTradeSumQuantityByActiveOrderId(int orderId) {
        return trades.stream().filter(t -> t.getActiveOrderId() == orderId).mapToInt(Trade::getQuantity).sum();
    }

    public void clear() {
        trades.clear();
    }
}
