package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 独立的撮合引擎测试类，不依赖项目中的任何其他类
 * 实现了限价单提交和撤单功能的测试
 */
public class StandaloneEngineTest {
    
    // 执行所有测试
    public static void main(String[] args) {
        try {
            System.out.println("===== 开始测试撮合引擎限价单和撤单功能 =====");
            
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
        }
    }
    
    //======== 测试用例 ========//
    
    /**
     * 测试添加单个买入限价单
     */
    private static void testSubmitSingleBuyLimitOrder() {
        System.out.println("测试添加单个买入限价单...");
        SimpleEngine engine = new SimpleEngine();
        
        // 提交一个买入限价单
        engine.submitLimitOrder(10, 100.0, Side.BUY);
        
        // 验证买单簿中有记录
        assert engine.getBuyBookSize() == 1 : "买单簿应该有1个价格层级";
        assert engine.getBuyBookQuantity() == 10 : "买单数量应该是10";
        
        System.out.println("通过");
    }
    
    /**
     * 测试添加单个卖出限价单
     */
    private static void testSubmitSingleSellLimitOrder() {
        System.out.println("测试添加单个卖出限价单...");
        SimpleEngine engine = new SimpleEngine();
        
        // 提交一个卖出限价单
        engine.submitLimitOrder(5, 200.0, Side.SELL);
        
        // 验证卖单簿中有记录
        assert engine.getSellBookSize() == 1 : "卖单簿应该有1个价格层级";
        assert engine.getSellBookQuantity() == 5 : "卖单数量应该是5";
        
        System.out.println("通过");
    }
    
    /**
     * 测试多个不同价格的限价单
     */
    private static void testMultiplePriceLevelOrders() {
        System.out.println("测试多个不同价格的限价单...");
        SimpleEngine engine = new SimpleEngine();
        
        // 提交多个不同价格的买单
        engine.submitLimitOrder(10, 100.0, Side.BUY);
        engine.submitLimitOrder(5, 99.0, Side.BUY);
        engine.submitLimitOrder(20, 98.0, Side.BUY);
        
        // 提交多个不同价格的卖单
        engine.submitLimitOrder(8, 101.0, Side.SELL);
        engine.submitLimitOrder(15, 102.0, Side.SELL);
        
        // 验证买单簿中有3个价格层级
        assert engine.getBuyBookSize() == 3 : "买单簿中应该有3个价格层级";
        
        // 验证卖单簿中有2个价格层级
        assert engine.getSellBookSize() == 2 : "卖单簿中应该有2个价格层级";
        
        System.out.println("通过");
    }
    
    /**
     * 测试限价单的成交匹配 - 完全成交
     */
    private static void testLimitOrderFullMatch() {
        System.out.println("测试限价单完全成交...");
        SimpleEngine engine = new SimpleEngine();
        
        // 添加一个卖单
        engine.submitLimitOrder(10, 100.0, Side.SELL);
        
        // 添加一个买单，买单价格高于卖单，应该完全成交
        engine.submitLimitOrder(10, 101.0, Side.BUY);
        
        // 验证卖单簿应该为空
        assert engine.getSellBookSize() == 0 : "卖单应该完全成交";
        
        // 验证买单簿应该为空（因为买单完全成交）
        assert engine.getBuyBookSize() == 0 : "买单应该完全成交";
        
        // 验证成交记录
        List<Trade> trades = engine.getTrades();
        assert trades.size() == 1 : "应该产生1笔成交";
        assert trades.get(0).getQuantity() == 10 : "成交数量应该是10";
        assert trades.get(0).getPrice() == 100.0 : "成交价格应该是100.0";
        
        System.out.println("通过");
    }
    
    /**
     * 测试限价单的成交匹配 - 部分成交
     */
    private static void testLimitOrderPartialMatch() {
        System.out.println("测试限价单部分成交...");
        SimpleEngine engine = new SimpleEngine();
        
        // 添加一个卖单
        engine.submitLimitOrder(10, 100.0, Side.SELL);
        
        // 添加一个数量较小的买单，买单价格高于卖单，应该完全成交，但卖单部分成交
        engine.submitLimitOrder(5, 101.0, Side.BUY);
        
        // 验证卖单簿中还剩余5个数量
        assert engine.getSellBookQuantity() == 5 : "卖单应该剩余5个数量";
        
        // 验证买单簿应该为空（因为买单完全成交）
        assert engine.getBuyBookSize() == 0 : "买单应该完全成交";
        
        // 验证成交记录
        List<Trade> trades = engine.getTrades();
        assert trades.size() == 1 : "应该产生1笔成交";
        assert trades.get(0).getQuantity() == 5 : "成交数量应该是5";
        
        System.out.println("通过");
    }
    
    /**
     * 测试限价单的成交匹配 - 无成交（价格不匹配）
     */
    private static void testLimitOrderNoMatchDueToPrice() {
        System.out.println("测试限价单价格不匹配无成交...");
        SimpleEngine engine = new SimpleEngine();
        
        // 添加一个卖单
        engine.submitLimitOrder(10, 100.0, Side.SELL);
        
        // 添加一个买单，但价格低于卖单，不应成交
        engine.submitLimitOrder(10, 99.0, Side.BUY);
        
        // 验证卖单簿中还有卖单
        assert engine.getSellBookQuantity() == 10 : "卖单不应该成交";
        
        // 验证买单簿中有买单
        assert engine.getBuyBookQuantity() == 10 : "买单不应该成交";
        
        // 验证没有成交记录
        List<Trade> trades = engine.getTrades();
        assert trades.isEmpty() : "不应该产生成交";
        
        System.out.println("通过");
    }
    
    /**
     * 测试撤销限价单
     */
    private static void testCancelLimitOrder() {
        System.out.println("测试撤销限价单...");
        SimpleEngine engine = new SimpleEngine();
        
        // 添加一个买单
        int orderId = engine.submitLimitOrder(10, 100.0, Side.BUY);
        
        // 撤销该买单
        engine.cancelOrder(Side.BUY, orderId);
        
        // 验证买单簿中该订单已被撤销
        assert engine.getBuyBookSize() == 0 : "买单应该被撤销";
        
        System.out.println("通过");
    }
    
    /**
     * 测试同一价格多个订单的时间优先级
     */
    private static void testTimePriorityForSamePriceOrders() {
        System.out.println("测试同一价格多个订单的时间优先级...");
        SimpleEngine engine = new SimpleEngine();
        
        // 添加3个相同价格的卖单
        engine.submitLimitOrder(5, 100.0, Side.SELL); // 订单1
        engine.submitLimitOrder(10, 100.0, Side.SELL); // 订单2
        engine.submitLimitOrder(15, 100.0, Side.SELL); // 订单3
        
        // 添加一个买单，数量只能匹配第一个卖单
        engine.submitLimitOrder(5, 100.0, Side.BUY);
        
        // 验证卖单簿中应该只剩下后两个订单的总量
        assert engine.getSellBookQuantity() == 25 : "卖单簿应该只剩下后两个订单的总量";
        
        // 验证成交记录
        List<Trade> trades = engine.getTrades();
        assert trades.size() == 1 : "应该产生1笔成交";
        assert trades.get(0).getQuantity() == 5 : "成交数量应该是5";
        
        System.out.println("通过");
    }
    
    /**
     * 测试价格优先级
     */
    private static void testPricePriorityForOrders() {
        System.out.println("测试价格优先级...");
        SimpleEngine engine = new SimpleEngine();
        
        // 添加3个不同价格的卖单
        engine.submitLimitOrder(10, 102.0, Side.SELL);
        engine.submitLimitOrder(10, 101.0, Side.SELL);
        engine.submitLimitOrder(10, 103.0, Side.SELL);
        
        // 添加一个买单，价格高于所有卖单，数量只能匹配两个最低价卖单
        engine.submitLimitOrder(20, 105.0, Side.BUY);
        
        // 验证只剩下最高价格的卖单
        assert engine.getSellBookQuantity() == 10 : "卖单簿应该只剩下最高价格的卖单";
        
        // 验证成交记录
        List<Trade> trades = engine.getTrades();
        assert trades.size() == 2 : "应该产生2笔成交";
        assert trades.get(0).getPrice() == 101.0 : "第一笔成交价格应该是101.0";
        assert trades.get(1).getPrice() == 102.0 : "第二笔成交价格应该是102.0";
        
        System.out.println("通过");
    }
    
    /**
     * 测试多次成交的限价单
     */
    private static void testMultipleMatchesForLimitOrder() {
        System.out.println("测试多次成交的限价单...");
        SimpleEngine engine = new SimpleEngine();
        
        // 添加多个不同价格的买单
        engine.submitLimitOrder(5, 98.0, Side.BUY);
        engine.submitLimitOrder(5, 99.0, Side.BUY);
        engine.submitLimitOrder(5, 100.0, Side.BUY);
        
        // 添加一个卖单，价格低于所有买单
        engine.submitLimitOrder(15, 97.0, Side.SELL);
        
        // 验证所有买单应该都被成交
        assert engine.getBuyBookSize() == 0 : "所有买单应该完全成交";
        
        // 验证卖单簿应该为空（因为卖单完全成交）
        assert engine.getSellBookSize() == 0 : "卖单应该完全成交";
        
        // 验证成交记录
        List<Trade> trades = engine.getTrades();
        assert trades.size() == 3 : "应该产生3笔成交";
        
        System.out.println("通过");
    }
    
    /**
     * 测试极端价格的限价单
     */
    private static void testExtremePriceLimitOrders() {
        System.out.println("测试极端价格的限价单...");
        SimpleEngine engine = new SimpleEngine();
        
        // 添加一个极高价格的卖单
        engine.submitLimitOrder(10, 1000000.0, Side.SELL);
        
        // 添加一个极低价格的买单
        engine.submitLimitOrder(10, 0.01, Side.BUY);
        
        // 验证两个订单都应该被正确添加，但不会成交
        assert engine.getSellBookQuantity() == 10 : "高价卖单应该不会成交";
        assert engine.getBuyBookQuantity() == 10 : "低价买单应该不会成交";
        
        // 验证没有成交记录
        List<Trade> trades = engine.getTrades();
        assert trades.isEmpty() : "不应该产生成交";
        
        System.out.println("通过");
    }
    
    /**
     * 测试边界数量的限价单
     */
    private static void testEdgeCaseQuantityLimitOrders() {
        System.out.println("测试边界数量的限价单...");
        SimpleEngine engine = new SimpleEngine();
        
        // 添加一个数量为1的卖单
        engine.submitLimitOrder(1, 100.0, Side.SELL);
        
        // 添加一个大数量的买单
        engine.submitLimitOrder(1000000, 101.0, Side.BUY);
        
        // 验证小数量卖单应该被完全成交
        assert engine.getSellBookSize() == 0 : "小数量卖单应该完全成交";
        
        // 大数量买单应该剩余999999
        assert engine.getBuyBookQuantity() == 999999 : "大数量买单应该剩余999999";
        
        // 验证成交记录
        List<Trade> trades = engine.getTrades();
        assert trades.size() == 1 : "应该产生1笔成交";
        assert trades.get(0).getQuantity() == 1 : "成交数量应该是1";
        
        System.out.println("通过");
    }
    
    //======== 模拟撮合引擎实现 ========//
    
    /**
     * 买卖方向枚举
     */
    enum Side {
        BUY, SELL
    }
    
    /**
     * 订单类
     */
    static class Order {
        private final int orderId;
        private final int quantity;
        private final double price;
        private final Side side;
        private int executedQuantity = 0;
        
        public Order(int orderId, int quantity, double price, Side side) {
            this.orderId = orderId;
            this.quantity = quantity;
            this.price = price;
            this.side = side;
        }
        
        public int getOrderId() {
            return orderId;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public int getRemainingQuantity() {
            return quantity - executedQuantity;
        }
        
        public double getPrice() {
            return price;
        }
        
        public Side getSide() {
            return side;
        }
        
        public void execute(int quantity) {
            executedQuantity += quantity;
            if (executedQuantity > this.quantity) {
                throw new IllegalStateException("执行数量超过订单数量");
            }
        }
        
        public boolean isCompleted() {
            return executedQuantity >= quantity;
        }
    }
    
    /**
     * 成交记录类
     */
    static class Trade {
        private final int activeOrderId;
        private final int passiveOrderId;
        private final double price;
        private final int quantity;
        
        public Trade(int activeOrderId, int passiveOrderId, double price, int quantity) {
            this.activeOrderId = activeOrderId;
            this.passiveOrderId = passiveOrderId;
            this.price = price;
            this.quantity = quantity;
        }
        
        public int getActiveOrderId() {
            return activeOrderId;
        }
        
        public int getPassiveOrderId() {
            return passiveOrderId;
        }
        
        public double getPrice() {
            return price;
        }
        
        public int getQuantity() {
            return quantity;
        }
    }
    
    /**
     * 价格层级类
     */
    static class PriceLevel {
        private final double price;
        private final List<Order> orders = new ArrayList<>();
        private int totalQuantity = 0;
        
        public PriceLevel(double price) {
            this.price = price;
        }
        
        public void addOrder(Order order) {
            orders.add(order);
            totalQuantity += order.getRemainingQuantity();
        }
        
        public Order getFirstOrder() {
            return orders.isEmpty() ? null : orders.get(0);
        }
        
        public void removeFirstOrder() {
            if (!orders.isEmpty()) {
                totalQuantity -= orders.get(0).getRemainingQuantity();
                orders.remove(0);
            }
        }
        
        public void removeOrder(int orderId) {
            for (int i = 0; i < orders.size(); i++) {
                if (orders.get(i).getOrderId() == orderId) {
                    totalQuantity -= orders.get(i).getRemainingQuantity();
                    orders.remove(i);
                    return;
                }
            }
        }
        
        public int getTotalQuantity() {
            return totalQuantity;
        }
        
        public boolean isEmpty() {
            return orders.isEmpty();
        }
        
        public double getPrice() {
            return price;
        }
    }
    
    /**
     * 简单撮合引擎实现
     */
    static class SimpleEngine {
        // 用于生成唯一订单ID
        private int nextOrderId = 1;
        
        // 买单簿和卖单簿，使用TreeMap按价格排序
        private final TreeMap<Double, PriceLevel> buyBook = new TreeMap<>((p1, p2) -> Double.compare(p2, p1)); // 买单降序
        private final TreeMap<Double, PriceLevel> sellBook = new TreeMap<>(); // 卖单升序
        
        // 所有订单的映射
        private final Map<Integer, Order> ordersMap = new HashMap<>();
        
        // 成交记录
        private final List<Trade> trades = new ArrayList<>();
        
        /**
         * 提交限价单
         */
        public int submitLimitOrder(int quantity, double price, Side side) {
            // 生成订单ID
            int orderId = nextOrderId++;
            
            // 创建订单
            Order order = new Order(orderId, quantity, price, side);
            ordersMap.put(orderId, order);
            
            // 尝试撮合订单
            matchOrder(order);
            
            // 如果订单没有完全成交，加入订单簿
            if (!order.isCompleted()) {
                if (side == Side.BUY) {
                    addToBuyBook(order);
                } else {
                    addToSellBook(order);
                }
            }
            
            return orderId;
        }
        
        /**
         * 撮合订单
         */
        private void matchOrder(Order order) {
            if (order.getSide() == Side.BUY) {
                // 买单与卖单簿撮合
                while (!order.isCompleted() && !sellBook.isEmpty()) {
                    Map.Entry<Double, PriceLevel> entry = sellBook.firstEntry();
                    double sellPrice = entry.getKey();
                    
                    // 检查价格是否匹配
                    if (order.getPrice() < sellPrice) {
                        break; // 买单价格低于最低卖单价格，无法成交
                    }
                    
                    PriceLevel priceLevel = entry.getValue();
                    Order sellOrder = priceLevel.getFirstOrder();
                    
                    // 计算成交数量
                    int tradeQuantity = Math.min(order.getRemainingQuantity(), sellOrder.getRemainingQuantity());
                    
                    // 创建成交记录
                    Trade trade = new Trade(order.getOrderId(), sellOrder.getOrderId(), sellPrice, tradeQuantity);
                    trades.add(trade);
                    
                    // 更新订单状态
                    order.execute(tradeQuantity);
                    sellOrder.execute(tradeQuantity);
                    
                    // 如果卖单已完全成交，从订单簿中移除
                    if (sellOrder.isCompleted()) {
                        priceLevel.removeFirstOrder();
                        if (priceLevel.isEmpty()) {
                            sellBook.remove(sellPrice);
                        }
                    }
                }
            } else {
                // 卖单与买单簿撮合
                while (!order.isCompleted() && !buyBook.isEmpty()) {
                    Map.Entry<Double, PriceLevel> entry = buyBook.firstEntry();
                    double buyPrice = entry.getKey();
                    
                    // 检查价格是否匹配
                    if (order.getPrice() > buyPrice) {
                        break; // 卖单价格高于最高买单价格，无法成交
                    }
                    
                    PriceLevel priceLevel = entry.getValue();
                    Order buyOrder = priceLevel.getFirstOrder();
                    
                    // 计算成交数量
                    int tradeQuantity = Math.min(order.getRemainingQuantity(), buyOrder.getRemainingQuantity());
                    
                    // 创建成交记录
                    Trade trade = new Trade(order.getOrderId(), buyOrder.getOrderId(), order.getPrice(), tradeQuantity);
                    trades.add(trade);
                    
                    // 更新订单状态
                    order.execute(tradeQuantity);
                    buyOrder.execute(tradeQuantity);
                    
                    // 如果买单已完全成交，从订单簿中移除
                    if (buyOrder.isCompleted()) {
                        priceLevel.removeFirstOrder();
                        if (priceLevel.isEmpty()) {
                            buyBook.remove(buyPrice);
                        }
                    }
                }
            }
        }
        
        /**
         * 添加订单到买单簿
         */
        private void addToBuyBook(Order order) {
            double price = order.getPrice();
            PriceLevel priceLevel = buyBook.get(price);
            
            if (priceLevel == null) {
                priceLevel = new PriceLevel(price);
                buyBook.put(price, priceLevel);
            }
            
            priceLevel.addOrder(order);
        }
        
        /**
         * 添加订单到卖单簿
         */
        private void addToSellBook(Order order) {
            double price = order.getPrice();
            PriceLevel priceLevel = sellBook.get(price);
            
            if (priceLevel == null) {
                priceLevel = new PriceLevel(price);
                sellBook.put(price, priceLevel);
            }
            
            priceLevel.addOrder(order);
        }
        
        /**
         * 取消订单
         */
        public void cancelOrder(Side side, int orderId) {
            Order order = ordersMap.get(orderId);
            if (order == null) {
                throw new IllegalArgumentException("订单不存在: " + orderId);
            }
            
            if (side == Side.BUY) {
                // 从买单簿中取消
                PriceLevel priceLevel = buyBook.get(order.getPrice());
                if (priceLevel != null) {
                    priceLevel.removeOrder(orderId);
                    if (priceLevel.isEmpty()) {
                        buyBook.remove(order.getPrice());
                    }
                }
            } else {
                // 从卖单簿中取消
                PriceLevel priceLevel = sellBook.get(order.getPrice());
                if (priceLevel != null) {
                    priceLevel.removeOrder(orderId);
                    if (priceLevel.isEmpty()) {
                        sellBook.remove(order.getPrice());
                    }
                }
            }
            
            // 从订单映射中移除
            ordersMap.remove(orderId);
        }
        
        /**
         * 获取买单簿中的价格层级数量
         */
        public int getBuyBookSize() {
            return buyBook.size();
        }
        
        /**
         * 获取卖单簿中的价格层级数量
         */
        public int getSellBookSize() {
            return sellBook.size();
        }
        
        /**
         * 获取买单簿中的总订单数量
         */
        public int getBuyBookQuantity() {
            int total = 0;
            for (PriceLevel level : buyBook.values()) {
                total += level.getTotalQuantity();
            }
            return total;
        }
        
        /**
         * 获取卖单簿中的总订单数量
         */
        public int getSellBookQuantity() {
            int total = 0;
            for (PriceLevel level : sellBook.values()) {
                total += level.getTotalQuantity();
            }
            return total;
        }
        
        /**
         * 获取成交记录
         */
        public List<Trade> getTrades() {
            return trades;
        }
    }
} 