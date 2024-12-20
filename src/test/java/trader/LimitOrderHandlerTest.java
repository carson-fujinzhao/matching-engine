package trader;

import core.Engine;
import core.bean.Side;
import it.unimi.dsi.fastutil.Pair;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LimitOrderHandlerTest {

    @Test
    void testMatchOrder() {
        LimitOrderHandler limitOrderHandler = new LimitOrderHandler();
        // Add a limit order
        limitOrderHandler.matchOrder(10, 100, Side.SELL);

        // Check if the price level was added correctly
        var priceLevel1 = Arrays.stream(Engine.SELL_ORDER_BOOK.getPriceLevels())
                .filter(p -> p.getPrice() == 100)
                .findFirst()
                .orElse(null);

        // test single order
        assertNotNull(priceLevel1, "Price level for price 100 should exist.");
        assertEquals(Pair.of(100000000, 10), priceLevel1.getEndOrder(), "Quantity at price level 100 should be 10.");


        // test multiple order
        limitOrderHandler.matchOrder(20, 101, Side.SELL);
        var priceLevel2 = Arrays.stream(Engine.SELL_ORDER_BOOK.getPriceLevels())
                .filter(p -> p.getPrice() == 101)
                .findFirst()
                .orElse(null);

        assertNotNull(priceLevel2, "Price level for price 101 should exist.");
        assertEquals(Pair.of(100000001, 20), priceLevel2.getEndOrder(), "Quantity at price level 101 should be 20.");

        limitOrderHandler.matchOrder(30, 101, Side.SELL);
        assertEquals(Pair.of(100000002, 30), priceLevel2.getEndOrder(), "Quantity at price level 101 should be 30.");

        // test transaction
        var trades = limitOrderHandler.matchOrder(20, 101, Side.BUY);

        assertEquals(1, trades.size(), "Number of trades should be 1.");
    }

    @Test
    void testContinuousOrderMatching() {
        LimitOrderHandler limitOrderHandler = new LimitOrderHandler();
        // Step 1: Add initial sell limit orders
        limitOrderHandler.matchOrder(10, 100, Side.SELL);
        limitOrderHandler.matchOrder(15, 100, Side.SELL);
        limitOrderHandler.matchOrder(20, 100, Side.SELL);

        // Validate the sell order book
        var priceLevel100 = Arrays.stream(Engine.SELL_ORDER_BOOK.getPriceLevels())
                .filter(p -> p.getPrice() == 100)
                .findFirst()
                .orElse(null);
        assertNotNull(priceLevel100, "Price level for price 100 should exist.");
        assertEquals(45, priceLevel100.getOpenQuantity(), "Total quantity at price level 100 should be 45.");

        // Step 2: Add buy limit orders to match against sell orders
        var trades1 = limitOrderHandler.matchOrder(10, 100, Side.BUY);
        assertEquals(1, trades1.size(), "Number of trades should be 1.");
        assertEquals(10, trades1.get(0).getQuantity(), "Trade quantity should be 10.");

        // Second buy order that exceeds the first sell order's quantity
        var trades2 = limitOrderHandler.matchOrder(20, 100, Side.BUY);
        assertEquals(2, trades2.size(), "Number of trades should be 2.");
        assertEquals(15, trades2.get(0).getQuantity(), "First trade quantity should be 15.");
        assertEquals(5, trades2.get(1).getQuantity(), "Second trade quantity should be 5.");

        // Validate remaining sell orders
        var remainingPriceLevel100 = Arrays.stream(Engine.SELL_ORDER_BOOK.getPriceLevels())
                .filter(p -> p.getPrice() == 100)
                .findFirst()
                .orElse(null);
        assertNotNull(remainingPriceLevel100, "Price level for price 100 should still exist.");
        assertEquals(15, remainingPriceLevel100.getOpenQuantity(), "Remaining quantity at price level 100 should be 15.");

        // Step 3: Add another buy order to fully consume remaining sell orders
        var trades3 = limitOrderHandler.matchOrder(20, 100, Side.BUY);
        assertEquals(1, trades3.size(), "Number of trades should be 1.");
        assertEquals(15, trades3.get(0).getQuantity(), "Trade quantity should be 15.");

        // Validate sell order book is empty
        var updatedPriceLevel100 = Arrays.stream(Engine.SELL_ORDER_BOOK.getPriceLevels())
                .filter(p -> p.getPrice() == 100)
                .findFirst()
                .orElse(null);
        assert updatedPriceLevel100 != null;
        assertTrue(updatedPriceLevel100.isEmpty(), "Price level for price 100 should be empty after all trades.");


        //step 4: buy order book left 1 order
        var updatedPriceLevelBuy100 = Arrays.stream(Engine.BUY_ORDER_BOOK.getPriceLevels())
                .filter(p -> p.getPrice() == 100)
                .findFirst()
                .orElse(null);
        assert updatedPriceLevelBuy100 != null;
        assertEquals(1, updatedPriceLevelBuy100.getOpenOrderCount(), "Open order count should be 1.");
        assertEquals(5, updatedPriceLevelBuy100.getOpenQuantity(), "Open quantity should be 5.");
    }

    @Test
    void testCancelOrder() {
        LimitOrderHandler limitOrderHandler = new LimitOrderHandler();
        limitOrderHandler.matchOrder(10, 100, Side.SELL);
        limitOrderHandler.matchOrder(30, 100, Side.SELL);
        limitOrderHandler.matchOrder(50, 100, Side.SELL);

        var idx1 = Engine.ORDER_BOOK(Side.SELL).getOrderIndex().getIndex(100000002);

        assertNotNull(idx1, "idx should be null.");
        assertEquals(2, idx1.coalesceValue(), "idx value not correct.");

        limitOrderHandler.cancelOrder(Side.SELL, 100000002);

        var priceLevel1 = Arrays.stream(Engine.SELL_ORDER_BOOK.getPriceLevels())
                .filter(p -> p.getPrice() == 100)
                .findFirst()
                .orElse(null);

        assertNotNull(priceLevel1, "Price level for price 100 should exist.");
        assertEquals(Pair.of(-1, -1), priceLevel1.getIndexEndOrder(0), "Quantity at price level 100 should be -1.");
    }
}
