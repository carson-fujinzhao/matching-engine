package core;

import core.bean.Side;

/**
 * 直接测试Engine类
 * 测试submitLimitOrder和cancelOrder方法的功能
 */
public class EngineDirectTest {
    
    private static Engine engine;
    
    public static void main(String[] args) {
        try {
            System.out.println("===== 开始测试撮合引擎限价单和撤单功能 =====");
            
            // 测试各个场景
            testSubmitSingleBuyLimitOrder();
            testSubmitSingleSellLimitOrder();
            testCancelLimitOrder();
            testLimitOrderMatchFullQty();
            testLimitOrderMatchPartialQty();
            testLimitOrderNoMatch();
            testMultiplePriceLevels();
            
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
    
    /**
     * 初始化撮合引擎
     */
    private static void setUp() {
        if (engine != null) {
            engine.shutdown();
        }
        engine = new Engine();
    }
    
    /**
     * 测试添加单个买入限价单
     */
    private static void testSubmitSingleBuyLimitOrder() {
        System.out.println("测试添加单个买入限价单...");
        setUp();
        
        // 提交一个买入限价单
        engine.submitLimitOrder(10, 100.0, Side.BUY);
        
        // 等待异步处理完成
        sleep(100);
        
        // 验证买单簿中应该有一个价格层级，且含有一个买单
        boolean hasOrder = false;
        for (int i = 0; i < Engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                hasOrder = true;
                break;
            }
        }
        
        assert hasOrder : "买单簿应该有订单";
        System.out.println("通过");
    }
    
    /**
     * 测试添加单个卖出限价单
     */
    private static void testSubmitSingleSellLimitOrder() {
        System.out.println("测试添加单个卖出限价单...");
        setUp();
        
        // 提交一个卖出限价单
        engine.submitLimitOrder(5, 200.0, Side.SELL);
        
        // 等待异步处理完成
        sleep(100);
        
        // 验证卖单簿中应该有一个价格层级，且含有一个卖单
        boolean hasOrder = false;
        for (int i = 0; i < Engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                hasOrder = true;
                break;
            }
        }
        
        assert hasOrder : "卖单簿应该有订单";
        System.out.println("通过");
    }
    
    /**
     * 测试撤销限价单
     */
    private static void testCancelLimitOrder() {
        System.out.println("测试撤销限价单...");
        setUp();
        
        // 提交一个买入限价单
        engine.submitLimitOrder(10, 100.0, Side.BUY);
        
        // 等待异步处理完成
        sleep(100);
        
        // 验证买单簿中应该有一个买单
        boolean hasOrderBefore = false;
        for (int i = 0; i < Engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                hasOrderBefore = true;
                break;
            }
        }
        
        assert hasOrderBefore : "买单簿应该有订单";
        
        // 撤销该买单（订单ID从100000000开始）
        engine.cancelOrder(Side.BUY, 100000000);
        
        // 等待异步处理完成
        sleep(100);
        
        // 验证买单簿中应该没有买单了
        boolean hasOrderAfter = false;
        for (int i = 0; i < Engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                hasOrderAfter = true;
                break;
            }
        }
        
        assert !hasOrderAfter : "买单应该被撤销";
        System.out.println("通过");
    }
    
    /**
     * 测试限价单完全成交
     */
    private static void testLimitOrderMatchFullQty() {
        System.out.println("测试限价单完全成交...");
        setUp();
        
        // 添加一个卖单
        engine.submitLimitOrder(10, 100.0, Side.SELL);
        sleep(100);
        
        // 添加一个买单，价格高于卖单，数量相等，应该完全成交
        engine.submitLimitOrder(10, 101.0, Side.BUY);
        sleep(100);
        
        // 验证两个订单簿都应该为空
        boolean emptySellBook = true;
        boolean emptyBuyBook = true;
        
        for (int i = 0; i < Engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                emptySellBook = false;
                break;
            }
        }
        
        for (int i = 0; i < Engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                emptyBuyBook = false;
                break;
            }
        }
        
        assert emptySellBook : "卖单簿应该为空";
        assert emptyBuyBook : "买单簿应该为空";
        
        System.out.println("通过");
    }
    
    /**
     * 测试限价单部分成交
     */
    private static void testLimitOrderMatchPartialQty() {
        System.out.println("测试限价单部分成交...");
        setUp();
        
        // 添加一个大量的卖单
        engine.submitLimitOrder(20, 100.0, Side.SELL);
        sleep(100);
        
        // 添加一个小量的买单，价格高于卖单，买单应该完全成交，卖单部分成交
        engine.submitLimitOrder(5, 101.0, Side.BUY);
        sleep(100);
        
        // 验证买单簿应该为空
        boolean emptyBuyBook = true;
        for (int i = 0; i < Engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                emptyBuyBook = false;
                break;
            }
        }
        
        // 验证卖单簿应该还有剩余卖单
        boolean hasSellOrder = false;
        for (int i = 0; i < Engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                hasSellOrder = true;
                break;
            }
        }
        
        assert emptyBuyBook : "买单簿应该为空";
        assert hasSellOrder : "卖单簿应该还有剩余订单";
        
        System.out.println("通过");
    }
    
    /**
     * 测试限价单无法成交的情况
     */
    private static void testLimitOrderNoMatch() {
        System.out.println("测试限价单无法成交的情况...");
        setUp();
        
        // 添加一个高价卖单
        engine.submitLimitOrder(10, 100.0, Side.SELL);
        sleep(100);
        
        // 添加一个低价买单，不应该成交
        engine.submitLimitOrder(10, 99.0, Side.BUY);
        sleep(100);
        
        // 验证两个订单簿都应该有订单
        boolean hasSellOrder = false;
        boolean hasBuyOrder = false;
        
        for (int i = 0; i < Engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                hasSellOrder = true;
                break;
            }
        }
        
        for (int i = 0; i < Engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                hasBuyOrder = true;
                break;
            }
        }
        
        assert hasSellOrder : "卖单簿应该有订单";
        assert hasBuyOrder : "买单簿应该有订单";
        
        System.out.println("通过");
    }
    
    /**
     * 测试多个价格层级的订单
     */
    private static void testMultiplePriceLevels() {
        System.out.println("测试多个价格层级的订单...");
        setUp();
        
        // 添加多个不同价格的买单
        engine.submitLimitOrder(10, 100.0, Side.BUY);
        engine.submitLimitOrder(5, 99.0, Side.BUY);
        engine.submitLimitOrder(20, 98.0, Side.BUY);
        sleep(100);
        
        // 添加多个不同价格的卖单
        engine.submitLimitOrder(8, 101.0, Side.SELL);
        engine.submitLimitOrder(15, 102.0, Side.SELL);
        sleep(100);
        
        // 验证买卖单簿应该都有订单
        boolean hasSellOrder = false;
        boolean hasBuyOrder = false;
        
        for (int i = 0; i < Engine.SELL_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.SELL_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.SELL_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                hasSellOrder = true;
                break;
            }
        }
        
        for (int i = 0; i < Engine.BUY_ORDER_BOOK.getPriceLevels().length; i++) {
            if (Engine.BUY_ORDER_BOOK.getPriceLevels()[i] != null && 
                !Engine.BUY_ORDER_BOOK.getPriceLevels()[i].isEmpty()) {
                hasBuyOrder = true;
                break;
            }
        }
        
        assert hasSellOrder : "卖单簿应该有订单";
        assert hasBuyOrder : "买单簿应该有订单";
        
        System.out.println("通过");
    }
    
    /**
     * 睡眠指定毫秒，用于等待异步处理完成
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
} 