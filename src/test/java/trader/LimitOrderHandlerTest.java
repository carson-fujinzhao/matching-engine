package trader;

import core.Engine;
import core.bean.Side;
import core.bean.Trade;
import core.price.PriceLevel;
import it.unimi.dsi.fastutil.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LimitOrderHandlerTest {

    private LimitOrderHandler limitOrderHandler;

    @BeforeEach
    void setUp() {
        // 在每个测试前创建新的引擎和处理器实例，确保测试互不影响
        limitOrderHandler = new LimitOrderHandler();
        // 由于新的OrderBook实现使用NavigableMap，不需要手动重置
        resetOrderBooks();
    }

    /**
     * 清空买单簿和卖单簿
     */
    private void resetOrderBooks() {
        // 清空所有价格级别
        Engine.BUY_ORDER_BOOK.getPriceLevels().clear();
        Engine.SELL_ORDER_BOOK.getPriceLevels().clear();
    }

    @Test
    void testAddSingleSellOrder() {
        // 添加单个卖单
        limitOrderHandler.matchOrder(10, 100, Side.SELL);

        // 验证卖单簿状态
        PriceLevel priceLevel = Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0);
        assertNotNull(priceLevel, "应该存在价格为100的价格级别");
        assertEquals(1, priceLevel.getOpenOrderCount(), "应该有1个订单");
        assertEquals(10, priceLevel.getOpenQuantity(), "订单总量应该为10");
        Pair<Integer, Integer> endOrder = priceLevel.getEndOrder();
        assertEquals(100000000, endOrder.first().intValue(), "订单ID应为100000000");
        assertEquals(10, endOrder.second().intValue(), "订单数量应为10");
    }

    @Test
    void testAddMultipleSellOrdersSamePrice() {
        // 添加多个相同价格的卖单
        limitOrderHandler.matchOrder(10, 100, Side.SELL);
        limitOrderHandler.matchOrder(20, 100, Side.SELL);
        limitOrderHandler.matchOrder(30, 100, Side.SELL);

        // 验证卖单簿状态
        PriceLevel priceLevel = Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0);
        assertNotNull(priceLevel, "应该存在价格为100的价格级别");
        assertEquals(3, priceLevel.getOpenOrderCount(), "应该有3个订单");
        assertEquals(60, priceLevel.getOpenQuantity(), "订单总量应该为60");
    }

    @Test
    void testAddSellOrdersDifferentPrices() {
        // 添加不同价格的卖单
        limitOrderHandler.matchOrder(10, 100, Side.SELL);
        limitOrderHandler.matchOrder(20, 101, Side.SELL);
        limitOrderHandler.matchOrder(30, 99, Side.SELL);

        // 验证不同价格级别
        PriceLevel priceLevel100 = Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0);
        PriceLevel priceLevel101 = Engine.SELL_ORDER_BOOK.getPriceLevels().get(101.0);
        PriceLevel priceLevel99 = Engine.SELL_ORDER_BOOK.getPriceLevels().get(99.0);

        assertNotNull(priceLevel100, "应该存在价格为100的价格级别");
        assertNotNull(priceLevel101, "应该存在价格为101的价格级别");
        assertNotNull(priceLevel99, "应该存在价格为99的价格级别");

        assertEquals(10, priceLevel100.getOpenQuantity(), "价格100的订单总量应为10");
        assertEquals(20, priceLevel101.getOpenQuantity(), "价格101的订单总量应为20");
        assertEquals(30, priceLevel99.getOpenQuantity(), "价格99的订单总量应为30");
    }

    @Test
    void testSimpleBuyOrderMatch() {
        // 先添加卖单
        limitOrderHandler.matchOrder(10, 100, Side.SELL);
        
        // 添加匹配的买单
        List<Trade> trades = limitOrderHandler.matchOrder(10, 100, Side.BUY);
        
        // 验证交易结果
        assertEquals(1, trades.size(), "应该生成1笔交易");
        Trade trade = trades.get(0);
        assertEquals(10, trade.getQuantity(), "交易数量应为10");
        assertEquals(100, trade.getPrice(), "交易价格应为100");
        assertEquals(100000000, trade.getPassiveOrderId(), "卖单ID应为100000000");
        assertEquals(100000001, trade.getActiveOrderId(), "买单ID应为100000001");
        
        // 验证卖单已被完全匹配（卖单簿中该价格级别应为空）
        assertNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0), "价格100的卖单应已全部匹配");
    }

    @Test
    void testPartialMatchBuyOrder() {
        // 先添加卖单
        limitOrderHandler.matchOrder(10, 100, Side.SELL);
        
        // 添加部分匹配的买单
        List<Trade> trades = limitOrderHandler.matchOrder(15, 100, Side.BUY);
        
        // 验证交易结果
        assertEquals(1, trades.size(), "应该生成1笔交易");
        assertEquals(10, trades.get(0).getQuantity(), "交易数量应为10");
        
        // 验证卖单已被完全匹配
        assertNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0), "价格100的卖单应已全部匹配");
        
        // 验证买单剩余部分添加到买单簿
        PriceLevel buyPriceLevel = Engine.BUY_ORDER_BOOK.getPriceLevels().get(100.0);
        assertNotNull(buyPriceLevel, "应该存在价格为100的买单价格级别");
        assertEquals(1, buyPriceLevel.getOpenOrderCount(), "应该有1个买单");
        assertEquals(5, buyPriceLevel.getOpenQuantity(), "买单剩余总量应为5");
    }

    @Test
    void testPriceTimePriorityMatchSellOrders() {
        // 添加多个不同价格的卖单
        limitOrderHandler.matchOrder(10, 100, Side.SELL); // 价格最低优先
        limitOrderHandler.matchOrder(20, 101, Side.SELL);
        limitOrderHandler.matchOrder(30, 102, Side.SELL);
        
        // 添加相同价格的多个卖单
        limitOrderHandler.matchOrder(15, 100, Side.SELL); // 价格相同，时间靠后
        
        // 添加足够大的买单，匹配多个价格级别
        List<Trade> trades = limitOrderHandler.matchOrder(30, 102, Side.BUY);
        
        // 验证交易结果
        assertEquals(3, trades.size(), "应该生成3笔交易");
        
        // 验证价格优先：第一笔交易应该是价格100的卖单
        assertEquals(100, trades.get(0).getPrice(), "第一笔交易价格应为100");
        assertEquals(10, trades.get(0).getQuantity(), "第一笔交易数量应为10");
        
        // 验证时间优先：第二笔交易应该是价格100的卖单
        assertEquals(100, trades.get(1).getPrice(), "第二笔交易价格应为100");
        assertEquals(15, trades.get(1).getQuantity(), "第二笔交易数量应为15");
        
        // 验证第三笔交易应该是价格101的卖单
        assertEquals(101, trades.get(2).getPrice(), "第三笔交易价格应为101");
        assertEquals(5, trades.get(2).getQuantity(), "第三笔交易数量应为5");
        
        // 验证买单已完全匹配
        assertEquals(30, trades.stream().mapToInt(Trade::getQuantity).sum(), "总交易数量应为30");
        
        // 验证卖单簿剩余状态
        assertNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0), "价格100的卖单应已全部匹配");
        assertNotNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(101.0), "价格101的卖单应该还存在");
        assertNotNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(102.0), "价格102的卖单应该还存在");
        
        // 验证买单簿状态（无剩余买单）
        assertNull(Engine.BUY_ORDER_BOOK.getPriceLevels().get(102.0), "价格102的买单应已全部匹配");
    }

    @Test
    void testCancelSellOrder() {
        // 添加卖单
        limitOrderHandler.matchOrder(10, 100, Side.SELL);
        int orderId = 100000000;
        
        // 验证卖单添加成功
        PriceLevel priceLevel = Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0);
        assertNotNull(priceLevel, "应该存在价格为100的价格级别");
        assertEquals(1, priceLevel.getOpenOrderCount(), "应该有1个订单");
        
        // 取消订单
        limitOrderHandler.cancelOrder(Side.SELL, orderId);
        
        // 验证订单已被取消
        assertNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0), "价格100的卖单应已被取消");
    }

    @Test
    void testCancelPartialOrdersAtPrice() {
        // 添加多个相同价格的卖单
        limitOrderHandler.matchOrder(10, 100, Side.SELL);
        int orderId1 = 100000000;
        limitOrderHandler.matchOrder(20, 100, Side.SELL);
        int orderId2 = 100000001;
        limitOrderHandler.matchOrder(30, 100, Side.SELL);
        int orderId3 = 100000002;
        
        // 取消第二个订单
        limitOrderHandler.cancelOrder(Side.SELL, orderId2);
        
        // 验证价格级别状态
        PriceLevel priceLevel = Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0);
        assertNotNull(priceLevel, "应该存在价格为100的价格级别");
        assertEquals(2, priceLevel.getOpenOrderCount(), "应该有2个订单");
        assertEquals(40, priceLevel.getOpenQuantity(), "订单总量应为40");
    }

    @Test
    void testComplexScenario() {
        // 场景1：添加多个卖单
        limitOrderHandler.matchOrder(10, 100, Side.SELL);
        limitOrderHandler.matchOrder(20, 101, Side.SELL);
        limitOrderHandler.matchOrder(30, 102, Side.SELL);
        
        // 验证卖单簿状态
        assertNotNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0), "应该存在价格为100的卖单");
        assertNotNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(101.0), "应该存在价格为101的卖单");
        assertNotNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(102.0), "应该存在价格为102的卖单");
        
        // 场景2：添加部分匹配的买单
        List<Trade> trades1 = limitOrderHandler.matchOrder(15, 101, Side.BUY);
        
        // 验证交易结果
        assertEquals(2, trades1.size(), "应该生成2笔交易");
        
        // 验证第一笔交易
        assertEquals(10, trades1.get(0).getQuantity(), "第一笔交易数量应为10");
        assertEquals(100, trades1.get(0).getPrice(), "第一笔交易价格应为100");
        
        // 验证第二笔交易
        assertEquals(5, trades1.get(1).getQuantity(), "第二笔交易数量应为5");
        assertEquals(101, trades1.get(1).getPrice(), "第二笔交易价格应为101");
        
        // 验证卖单簿状态
        assertNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0), "价格100的卖单应已全部匹配");
        assertNotNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(101.0), "应该存在价格为101的卖单");
        
        // 验证买单簿状态 - 由于完全匹配，不应该有剩余买单
        assertNull(Engine.BUY_ORDER_BOOK.getPriceLevels().get(101.0), "不应该有剩余买单");
        
        // 场景3：添加不匹配的卖单
        limitOrderHandler.matchOrder(25, 103, Side.SELL);
        
        // 验证卖单簿状态
        assertNotNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(103.0), "应该存在价格为103的卖单");
        
        // 场景4：添加完全匹配的买单
        List<Trade> trades2 = limitOrderHandler.matchOrder(20, 103, Side.BUY);
        
        // 验证交易结果
        assertEquals(2, trades2.size(), "应该生成2笔交易");
        
        // 验证第一笔交易
        assertEquals(15, trades2.get(0).getQuantity(), "第一笔交易数量应为15");
        assertEquals(101, trades2.get(0).getPrice(), "第一笔交易价格应为101");
        
        // 验证第二笔交易
        assertEquals(5, trades2.get(1).getQuantity(), "第二笔交易数量应为5");
        assertEquals(102, trades2.get(1).getPrice(), "第二笔交易价格应为102");
        
        // 验证卖单簿状态
        assertNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(101.0), "价格101的卖单应已全部匹配");
        assertNotNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(102.0), "价格102的卖单应该还存在");
    }

    @Test
    void testMarketStressWithMultipleOrders() {
        // 添加大量买卖订单，模拟市场压力
        int orderCount = 20;
        
        // 清空之前的所有订单
        resetOrderBooks();
        
        // 添加卖单，价格为100，确保卖单簿不为空
        limitOrderHandler.matchOrder(50, 100, Side.SELL);
        
        // 验证卖单添加成功
        assertNotNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0), "应该存在价格为100的卖单");
        
        // 添加买单，价格为100，应该与卖单匹配
        List<Trade> trades = limitOrderHandler.matchOrder(30, 100, Side.BUY);
        
        // 验证交易生成
        assertFalse(trades.isEmpty(), "买单应该产生交易");
        assertEquals(30, trades.get(0).getQuantity(), "交易数量应为30");
        
        // 验证卖单簿状态 - 卖单还有20单未匹配
        PriceLevel sellLevel = Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0);
        assertNotNull(sellLevel, "价格100的卖单应该还存在");
        assertEquals(20, sellLevel.getOpenQuantity(), "卖单剩余数量应为20");
        
        // 添加低价买单，不会匹配
        trades = limitOrderHandler.matchOrder(10, 90, Side.BUY);
        assertTrue(trades.isEmpty(), "低价买单不应产生交易");
        
        // 验证买单添加成功
        assertNotNull(Engine.BUY_ORDER_BOOK.getPriceLevels().get(90.0), "应该存在价格为90的买单");
    }

    @Test
    void testOrderCancellationEdgeCases() {
        // 测试取消不存在的订单
        limitOrderHandler.cancelOrder(Side.SELL, 999999); // 不存在的订单ID
        
        // 测试取消订单后再次取消同一订单
        limitOrderHandler.matchOrder(10, 100, Side.SELL);
        int orderId = 100000000;
        limitOrderHandler.cancelOrder(Side.SELL, orderId);
        limitOrderHandler.cancelOrder(Side.SELL, orderId); // 再次取消
        
        // 测试添加订单后匹配一部分再取消
        limitOrderHandler.matchOrder(20, 100, Side.SELL);
        orderId = 100000001;
        limitOrderHandler.matchOrder(10, 100, Side.BUY); // 部分匹配
        limitOrderHandler.cancelOrder(Side.SELL, orderId);
        
        // 验证最终状态
        assertNull(Engine.SELL_ORDER_BOOK.getPriceLevels().get(100.0), "价格100的卖单应已全部匹配或取消");
    }
}
