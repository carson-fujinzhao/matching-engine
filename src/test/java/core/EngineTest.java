package core;

import core.bean.Side;
import core.order.OrderBook;

/**
 * Engine类测试
 * 主要测试submitLimitOrder和cancelOrder方法在各种撮合场景中的行为
 */
public class EngineTest {
    
    private static MockEngine engine;
    
    public static void main(String[] args) {
        try {
            System.out.println("===== 开始测试Engine类 =====");
            
            // 测试各个场景
            testSubmitSingleBuyLimitOrder();
            testSubmitSingleSellLimitOrder();
            testMultiplePriceLevelOrders();
            testLimitOrderFullMatch();
            testLimitOrderPartialMatch();
            testLimitOrderNoMatchDueToPrice();
            testCancelLimitOrder();
            testTimePriorityForSamePriceOrders();
            testPricePriorityForOrders();
            testMultipleMatchesForLimitOrder();
            testExtremePriceLimitOrders();
            testEdgeCaseQuantityLimitOrders();
            
            System.out.println("===== 所有测试通过 =====");
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (engine != null) {
                engine.shutdown();
            }
        }
    }
    
    private static void setUp() {
        // 初始化撮合引擎
        engine = new MockEngine();
    }
    
    /**
     * 测试添加单个买入限价单
     */
    private static void testSubmitSingleBuyLimitOrder() throws Exception {
        System.out.println("测试添加单个买入限价单...");
        setUp();
        
        // 提交一个买入限价单
        engine.submitLimitOrder(10, 100.0, Side.BUY);
        
        // 验证买单簿中有记录
        assert !engine.BUY_ORDER_BOOK.getPriceLevels()[0].isEmpty() : "买单簿应该有订单";
        assert engine.BUY_ORDER_BOOK.getPriceLevels()[0].getOpenQuantity() == 10 : "买单数量应该是10";
        
        System.out.println("通过");
    }
    
    /**
     * 测试添加单个卖出限价单
     */
    private static void testSubmitSingleSellLimitOrder() throws Exception {
        System.out.println("测试添加单个卖出限价单...");
        setUp();
        
        // 提交一个卖出限价单
        engine.submitLimitOrder(5, 200.0, Side.SELL);
        
        // 验证卖单簿中有记录
        assert !engine.SELL_ORDER_BOOK.getPriceLevels()[0].isEmpty() : "卖单簿应该有订单";
        assert engine.SELL_ORDER_BOOK.getPriceLevels()[0].getOpenQuantity() == 5 : "卖单数量应该是5";
        
        System.out.println("通过");
    }
    
    /**
     * 测试多个不同价格的限价单
     */
    private static void testMultiplePriceLevelOrders() throws Exception {
        System.out.println("测试多个不同价格的限价单...");
        setUp();
        
        // 提交多个不同价格的买单
        engine.submitLimitOrder(10, 100.0, Side.BUY);
        engine.submitLimitOrder(5, 99.0, Side.BUY);
        engine.submitLimitOrder(20, 98.0, Side.BUY);
        
        // 提交多个不同价格的卖单
        engine.submitLimitOrder(8, 101.0, Side.SELL);
        engine.submitLimitOrder(15, 102.0, Side.SELL);
        
        // 验证买单簿中有3个价格层级
        int buyOrderCount = 0;
        for (int i = 0; i < engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && !engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                buyOrderCount++;
            }
        }
        assert buyOrderCount == 3 : "买单簿中应该有3个价格层级";
        
        // 验证卖单簿中有2个价格层级
        int sellOrderCount = 0;
        for (int i = 0; i < engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && !engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                sellOrderCount++;
            }
        }
        assert sellOrderCount == 2 : "卖单簿中应该有2个价格层级";
        
        System.out.println("通过");
    }
    
    /**
     * 测试限价单的成交匹配 - 完全成交
     */
    private static void testLimitOrderFullMatch() throws Exception {
        System.out.println("测试限价单完全成交...");
        setUp();
        
        // 添加一个卖单
        engine.submitLimitOrder(10, 100.0, Side.SELL);
        
        // 添加一个买单，买单价格高于卖单，应该完全成交
        engine.submitLimitOrder(10, 101.0, Side.BUY);
        
        // 验证卖单簿应该为空
        boolean emptySellBook = true;
        for (int i = 0; i < engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && !engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                emptySellBook = false;
                break;
            }
        }
        assert emptySellBook : "卖单应该完全成交";
        
        // 验证买单簿应该为空（因为买单完全成交）
        boolean emptyBuyBook = true;
        for (int i = 0; i < engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && !engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                emptyBuyBook = false;
                break;
            }
        }
        assert emptyBuyBook : "买单应该完全成交";
        
        System.out.println("通过");
    }
    
    /**
     * 测试限价单的成交匹配 - 部分成交
     */
    private static void testLimitOrderPartialMatch() throws Exception {
        System.out.println("测试限价单部分成交...");
        setUp();
        
        // 添加一个卖单
        engine.submitLimitOrder(10, 100.0, Side.SELL);
        
        // 添加一个数量较小的买单，买单价格高于卖单，应该完全成交，但卖单部分成交
        engine.submitLimitOrder(5, 101.0, Side.BUY);
        
        // 验证卖单簿中还剩余5个数量
        int remainingSellQuantity = 0;
        for (int i = 0; i < engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && !engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                remainingSellQuantity += engine.SELL_ORDER_BOOK.getPriceLevels()[i].getOpenQuantity();
            }
        }
        assert remainingSellQuantity == 5 : "卖单应该剩余5个数量";
        
        // 验证买单簿应该为空（因为买单完全成交）
        boolean emptyBuyBook = true;
        for (int i = 0; i < engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && !engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                emptyBuyBook = false;
                break;
            }
        }
        assert emptyBuyBook : "买单应该完全成交";
        
        System.out.println("通过");
    }
    
    /**
     * 测试限价单的成交匹配 - 无成交（价格不匹配）
     */
    private static void testLimitOrderNoMatchDueToPrice() throws Exception {
        System.out.println("测试限价单价格不匹配无成交...");
        setUp();
        
        // 添加一个卖单
        engine.submitLimitOrder(10, 100.0, Side.SELL);
        
        // 添加一个买单，但价格低于卖单，不应成交
        engine.submitLimitOrder(10, 99.0, Side.BUY);
        
        // 验证卖单簿中还有卖单
        int sellQuantity = 0;
        for (int i = 0; i < engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && !engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                sellQuantity += engine.SELL_ORDER_BOOK.getPriceLevels()[i].getOpenQuantity();
            }
        }
        assert sellQuantity == 10 : "卖单不应该成交";
        
        // 验证买单簿中有买单
        int buyQuantity = 0;
        for (int i = 0; i < engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && !engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                buyQuantity += engine.BUY_ORDER_BOOK.getPriceLevels()[i].getOpenQuantity();
            }
        }
        assert buyQuantity == 10 : "买单不应该成交";
        
        System.out.println("通过");
    }
    
    /**
     * 测试撤销限价单
     */
    private static void testCancelLimitOrder() throws Exception {
        System.out.println("测试撤销限价单...");
        setUp();
        
        // 添加一个买单
        engine.submitLimitOrder(10, 100.0, Side.BUY);
        
        // 获取买单ID（第一个限价单ID应该是从100000000开始）
        int orderId = 100000000;
        
        // 撤销该买单
        engine.cancelOrder(Side.BUY, orderId);
        
        // 验证买单簿中该订单已被撤销
        boolean emptyBuyBook = true;
        for (int i = 0; i < engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && !engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                emptyBuyBook = false;
                break;
            }
        }
        assert emptyBuyBook : "买单应该被撤销";
        
        System.out.println("通过");
    }
    
    /**
     * 测试同一价格多个订单的时间优先级
     */
    private static void testTimePriorityForSamePriceOrders() throws Exception {
        System.out.println("测试同一价格多个订单的时间优先级...");
        setUp();
        
        // 添加3个相同价格的卖单
        engine.submitLimitOrder(5, 100.0, Side.SELL); // 订单1
        engine.submitLimitOrder(10, 100.0, Side.SELL); // 订单2
        engine.submitLimitOrder(15, 100.0, Side.SELL); // 订单3
        
        // 添加一个买单，数量只能匹配第一个卖单
        engine.submitLimitOrder(5, 100.0, Side.BUY);
        
        // 验证卖单簿中应该只剩下后两个订单的总量
        int remainingSellQuantity = 0;
        for (int i = 0; i < engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && !engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                remainingSellQuantity += engine.SELL_ORDER_BOOK.getPriceLevels()[i].getOpenQuantity();
            }
        }
        assert remainingSellQuantity == 25 : "卖单簿应该只剩下后两个订单的总量";
        
        System.out.println("通过");
    }
    
    /**
     * 测试价格优先级
     */
    private static void testPricePriorityForOrders() throws Exception {
        System.out.println("测试价格优先级...");
        setUp();
        
        // 添加3个不同价格的卖单
        engine.submitLimitOrder(10, 102.0, Side.SELL);
        engine.submitLimitOrder(10, 101.0, Side.SELL);
        engine.submitLimitOrder(10, 103.0, Side.SELL);
        
        // 添加一个买单，价格高于所有卖单，数量只能匹配两个最低价卖单
        engine.submitLimitOrder(20, 105.0, Side.BUY);
        
        // 验证只剩下最高价格的卖单
        int remainingSellQuantity = 0;
        for (int i = 0; i < engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && !engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                remainingSellQuantity += engine.SELL_ORDER_BOOK.getPriceLevels()[i].getOpenQuantity();
            }
        }
        assert remainingSellQuantity == 10 : "卖单簿应该只剩下最高价格的卖单";
        
        System.out.println("通过");
    }
    
    /**
     * 测试多次成交的限价单
     */
    private static void testMultipleMatchesForLimitOrder() throws Exception {
        System.out.println("测试多次成交的限价单...");
        setUp();
        
        // 添加多个不同价格的买单
        engine.submitLimitOrder(5, 98.0, Side.BUY);
        engine.submitLimitOrder(5, 99.0, Side.BUY);
        engine.submitLimitOrder(5, 100.0, Side.BUY);
        
        // 添加一个卖单，价格低于所有买单
        engine.submitLimitOrder(15, 97.0, Side.SELL);
        
        // 验证所有买单应该都被成交
        boolean emptyBuyBook = true;
        for (int i = 0; i < engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && !engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                emptyBuyBook = false;
                break;
            }
        }
        assert emptyBuyBook : "所有买单应该完全成交";
        
        // 验证卖单簿应该为空（因为卖单完全成交）
        boolean emptySellBook = true;
        for (int i = 0; i < engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && !engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                emptySellBook = false;
                break;
            }
        }
        assert emptySellBook : "卖单应该完全成交";
        
        System.out.println("通过");
    }
    
    /**
     * 测试极端价格的限价单
     */
    private static void testExtremePriceLimitOrders() throws Exception {
        System.out.println("测试极端价格的限价单...");
        setUp();
        
        // 添加一个极高价格的卖单
        engine.submitLimitOrder(10, 1000000.0, Side.SELL);
        
        // 添加一个极低价格的买单
        engine.submitLimitOrder(10, 0.01, Side.BUY);
        
        // 验证两个订单都应该被正确添加，但不会成交
        int sellQuantity = 0;
        for (int i = 0; i < engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && !engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                sellQuantity += engine.SELL_ORDER_BOOK.getPriceLevels()[i].getOpenQuantity();
            }
        }
        assert sellQuantity == 10 : "高价卖单应该不会成交";
        
        int buyQuantity = 0;
        for (int i = 0; i < engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && !engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                buyQuantity += engine.BUY_ORDER_BOOK.getPriceLevels()[i].getOpenQuantity();
            }
        }
        assert buyQuantity == 10 : "低价买单应该不会成交";
        
        System.out.println("通过");
    }
    
    /**
     * 测试边界数量的限价单
     */
    private static void testEdgeCaseQuantityLimitOrders() throws Exception {
        System.out.println("测试边界数量的限价单...");
        setUp();
        
        // 添加一个数量为1的卖单
        engine.submitLimitOrder(1, 100.0, Side.SELL);
        
        // 添加一个大数量的买单
        engine.submitLimitOrder(1000000, 101.0, Side.BUY);
        
        // 验证小数量卖单应该被完全成交
        boolean emptySellBook = true;
        for (int i = 0; i < engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && !engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                emptySellBook = false;
                break;
            }
        }
        assert emptySellBook : "小数量卖单应该完全成交";
        
        // 大数量买单应该剩余999999
        int remainingBuyQuantity = 0;
        for (int i = 0; i < engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && !engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                remainingBuyQuantity += engine.BUY_ORDER_BOOK.getPriceLevels()[i].getOpenQuantity();
            }
        }
        assert remainingBuyQuantity == 999999 : "大数量买单应该剩余999999";
        
        System.out.println("通过");
    }
} 