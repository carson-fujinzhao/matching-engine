package core;

import core.bean.Side;
import core.order.OrderBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineTest {

    @BeforeEach
    void setUp() {
        // Reset the order books before each test to ensure isolation
        Engine.SELL_ORDER_BOOK = new OrderBook(Side.SELL);
        Engine.BUY_ORDER_BOOK = new OrderBook(Side.BUY);
    }

    @Test
    void testCurrProcOrderBookWithInvalidSide() {
        // Test CURR_PROC_ORDER_BOOK with an invalid side
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Engine.ORDER_BOOK(null);
        });
        String expectedMessage = "Unsupported order side";
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should match the expected message exactly");
    }
}