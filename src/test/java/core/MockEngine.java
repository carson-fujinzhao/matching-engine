package core;

import core.bean.Order;
import core.bean.Side;
import core.bean.Trade;
import core.order.OrderBook;
import trader.bean.LimitOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于测试的Engine模拟类，模拟撮合引擎的基本行为，不依赖外部库
 */
public class MockEngine {
    // 静态订单簿，可以被测试直接访问
    public static OrderBook SELL_ORDER_BOOK;
    public static OrderBook BUY_ORDER_BOOK;
    
    // 订单ID生成
    private int limitOrderId = 100000000;
    
    // 存储提交的订单，用于测试验证
    private Map<Integer, Order> orders = new HashMap<>();
    
    // 存储产生的成交记录，用于测试验证
    private List<Trade> trades = new ArrayList<>();
    
    public MockEngine() {
        // 初始化订单簿
        SELL_ORDER_BOOK = new MockOrderBook(Side.SELL);
        BUY_ORDER_BOOK = new MockOrderBook(Side.BUY);
    }
    
    /**
     * 获取与给定Side相反的订单簿
     */
    public static OrderBook OPPOSITE_ORDER_BOOK(Side side) {
        if (side == null) {
            throw new IllegalArgumentException("Unsupported order side");
        }
        return side == Side.SELL ? BUY_ORDER_BOOK : SELL_ORDER_BOOK;
    }

    /**
     * 获取给定Side的订单簿
     */
    public static OrderBook ORDER_BOOK(Side side) {
        if (side == null) {
            throw new IllegalArgumentException("Unsupported order side");
        }
        return side == Side.SELL ? SELL_ORDER_BOOK : BUY_ORDER_BOOK;
    }

    /**
     * 提交限价单的模拟实现
     */
    public void submitLimitOrder(int quantity, double price, Side side) {
        // 创建限价单
        LimitOrder order = new LimitOrder(limitOrderId++, quantity, price, side);
        orders.put(order.getOrderId(), order);
        
        // 直接调用撮合逻辑，不需要通过Disruptor
        // 1. 尝试与对手盘撮合
        List<Trade> newTrades = OPPOSITE_ORDER_BOOK(side).matchOrder(order);
        trades.addAll(newTrades);
        
        // 2. 如果订单没有完全成交，放入订单簿
        if (!order.isCompleted()) {
            ORDER_BOOK(side).onNewOrder(order);
        }
    }
    
    /**
     * 取消订单的模拟实现
     */
    public void cancelOrder(Side side, int orderId) {
        ORDER_BOOK(side).onCancelOrder(orderId);
        orders.remove(orderId);
    }
    
    /**
     * 关闭引擎
     */
    public void shutdown() {
        // 模拟实现不需要实际关闭任何资源
    }
    
    /**
     * 获取订单
     */
    public Order getOrder(int orderId) {
        return orders.get(orderId);
    }
    
    /**
     * 获取成交记录
     */
    public List<Trade> getTrades() {
        return trades;
    }
    
    /**
     * 清除所有数据，用于测试前重置状态
     */
    public void clear() {
        orders.clear();
        trades.clear();
        SELL_ORDER_BOOK = new MockOrderBook(Side.SELL);
        BUY_ORDER_BOOK = new MockOrderBook(Side.BUY);
    }
} 