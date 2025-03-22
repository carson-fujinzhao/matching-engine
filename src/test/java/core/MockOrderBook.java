package core;

import core.bean.Order;
import core.bean.Side;
import core.bean.Trade;
import core.order.OrderBook;
import core.price.PriceLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 用于测试的OrderBook模拟类，实现了OrderBook的接口，但不依赖于外部库
 */
public class MockOrderBook extends OrderBook {
    
    // 存储价格层级的映射
    private Map<Double, MockPriceLevel> priceLevels = new HashMap<>();
    
    // 存储订单ID到价格的映射，用于快速查找
    private Map<Integer, Double> orderIdToPriceMap = new HashMap<>();
    
    // 按照买卖方向排序的价格层级
    private TreeMap<Double, MockPriceLevel> sortedPriceLevels;
    
    // 保存买卖方向
    private final Side orderSide;
    
    public MockOrderBook(Side side) {
        super(side);
        this.orderSide = side;
        
        // 根据买卖方向确定排序方式
        if (side == Side.BUY) {
            // 买单按价格降序排列（高价优先）
            sortedPriceLevels = new TreeMap<>((p1, p2) -> Double.compare(p2, p1));
        } else {
            // 卖单按价格升序排列（低价优先）
            sortedPriceLevels = new TreeMap<>();
        }
    }
    
    @Override
    public List<Trade> matchOrder(Order order) {
        if (orderSide.equals(order.getSide())) {
            // 同方向订单不能撮合
            return new ArrayList<>();
        }
        
        List<Trade> trades = new ArrayList<>();
        
        // 根据价格优先级遍历价格层级
        while (!sortedPriceLevels.isEmpty()) {
            Map.Entry<Double, MockPriceLevel> entry = sortedPriceLevels.firstEntry();
            Double price = entry.getKey();
            MockPriceLevel priceLevel = entry.getValue();
            
            // 检查价格是否可以撮合
            if (orderSide == Side.BUY && price < order.getPrice()) {
                // 买方订单簿价格小于卖单价格，无法撮合
                break;
            } else if (orderSide == Side.SELL && price > order.getPrice()) {
                // 卖方订单簿价格大于买单价格，无法撮合
                break;
            }
            
            // 在当前价格层级进行撮合
            priceLevel.matchOrder(order, trades);
            
            // 如果价格层级已空，移除
            if (priceLevel.isEmpty()) {
                sortedPriceLevels.remove(price);
                priceLevels.remove(price);
            }
            
            // 如果活动订单已完全成交，终止撮合
            if (order.isCompleted()) {
                break;
            }
            
            // 如果没有更多价格层级，终止撮合
            if (sortedPriceLevels.isEmpty()) {
                break;
            }
        }
        
        return trades;
    }
    
    @Override
    public void onCancelOrder(int orderId) {
        // 查找订单所在的价格层级
        Double price = orderIdToPriceMap.get(orderId);
        if (price == null) {
            throw new java.util.NoSuchElementException("Order not found: " + orderId);
        }
        
        // 获取价格层级
        MockPriceLevel priceLevel = priceLevels.get(price);
        if (priceLevel != null) {
            // 取消订单
            priceLevel.cancelOrder(orderId);
            
            // 如果价格层级已空，移除
            if (priceLevel.isEmpty()) {
                sortedPriceLevels.remove(price);
                priceLevels.remove(price);
            }
            
            // 移除订单ID映射
            orderIdToPriceMap.remove(orderId);
        }
    }
    
    @Override
    public void onNewOrder(Order order) {
        double price = order.getPrice();
        
        // 获取或创建价格层级
        MockPriceLevel priceLevel = priceLevels.get(price);
        if (priceLevel == null) {
            priceLevel = new MockPriceLevel(orderSide, price);
            priceLevels.put(price, priceLevel);
            sortedPriceLevels.put(price, priceLevel);
        }
        
        // 添加订单到价格层级
        priceLevel.addOrder(order);
        
        // 添加订单ID到价格的映射
        orderIdToPriceMap.put(order.getOrderId(), price);
    }
    
    /**
     * 获取模拟的价格层级数组，用于测试
     */
    @Override
    public PriceLevel[] getPriceLevels() {
        PriceLevel[] result = new PriceLevel[EngineConstants.MAX_PRICE_LEVELS];
        int i = 0;
        for (MockPriceLevel level : priceLevels.values()) {
            if (i < result.length) {
                result[i++] = level;
            }
        }
        return result;
    }
    
    /**
     * 内部类：模拟的价格层级，不依赖于外部库
     */
    public static class MockPriceLevel extends PriceLevel {
        private double price;
        private final Side side;
        private Map<Integer, Integer> orders = new HashMap<>(); // orderId -> quantity
        private List<Integer> orderIds = new ArrayList<>(); // 保持时间优先顺序
        private int openQuantity = 0;
        
        public MockPriceLevel(Side side, double price) {
            super(side);
            this.side = side;
            this.price = price;
        }
        
        public void addOrder(Order order) {
            int orderId = order.getOrderId();
            int quantity = order.getPendingMatchQuantity();
            
            // 添加订单
            orders.put(orderId, quantity);
            orderIds.add(orderId);
            
            // 更新总量
            openQuantity += quantity;
        }
        
        public void cancelOrder(int orderId) {
            // 检查订单是否存在
            Integer quantity = orders.get(orderId);
            if (quantity != null) {
                // 更新总量
                openQuantity -= quantity;
                
                // 移除订单
                orders.remove(orderId);
                orderIds.remove(Integer.valueOf(orderId));
            }
        }
        
        @Override
        public void matchOrder(Order order, List<Trade> trades) {
            // 如果没有订单，直接返回
            if (orderIds.isEmpty()) {
                return;
            }
            
            // 按照时间优先级遍历订单
            List<Integer> toRemove = new ArrayList<>();
            for (int i = 0; i < orderIds.size(); i++) {
                int passiveOrderId = orderIds.get(i);
                int passiveQuantity = orders.get(passiveOrderId);
                
                // 计算成交数量
                int tradeQuantity = Math.min(order.getPendingMatchQuantity(), passiveQuantity);
                
                // 创建成交记录
                trades.add(new Trade(order.getOrderId(), passiveOrderId, price, tradeQuantity));
                
                // 更新被动订单数量
                orders.put(passiveOrderId, passiveQuantity - tradeQuantity);
                openQuantity -= tradeQuantity;
                
                // 更新活动订单状态
                order.makeMatched(tradeQuantity);
                
                // 如果被动订单已完全成交，标记为移除
                if (passiveQuantity - tradeQuantity == 0) {
                    toRemove.add(passiveOrderId);
                }
                
                // 如果活动订单已完全成交，终止撮合
                if (order.isCompleted()) {
                    break;
                }
            }
            
            // 移除已完全成交的订单
            for (Integer orderId : toRemove) {
                orders.remove(orderId);
                orderIds.remove(orderId);
            }
        }
        
        @Override
        public boolean isEmpty() {
            return orders.isEmpty();
        }
        
        @Override
        public int getOpenQuantity() {
            return openQuantity;
        }
        
        @Override
        public double getPrice() {
            return price;
        }
    }
} 