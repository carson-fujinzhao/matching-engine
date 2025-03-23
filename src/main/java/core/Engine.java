package core;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import core.bean.Order;
import core.bean.Side;
import core.event.OrderMatchEvent;
import core.event.OrderMatchEventFactory;
import core.event.OrderMatchEventHandler;
import core.event.OrderResult;
import core.order.OrderBook;
import trader.TradePersistence;
import trader.bean.LimitOrder;
import trader.bean.MarketOrder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Engine {
    public static OrderBook SELL_ORDER_BOOK;
    public static OrderBook BUY_ORDER_BOOK;
    
    // 撮合引擎Disruptor相关组件
    private final Disruptor<OrderMatchEvent> disruptor;
    private final RingBuffer<OrderMatchEvent> ringBuffer;

    // 订单ID生成
    private int marketOrderId = 0;
    private int limitOrderId = 100000000;

    public Engine() {
        SELL_ORDER_BOOK = new OrderBook(Side.SELL);
        BUY_ORDER_BOOK = new OrderBook(Side.BUY);
        
        // 初始化交易持久化
        TradePersistence tradePersistence = new TradePersistence();
        
        // 初始化Disruptor
        disruptor = new Disruptor<>(
                new OrderMatchEventFactory(),
                1024,
                Executors.defaultThreadFactory(),
                ProducerType.MULTI,
                new BusySpinWaitStrategy()
        );
        
        // 设置事件处理器
        disruptor.handleEventsWith(new OrderMatchEventHandler(tradePersistence));
        
        // 启动Disruptor
        disruptor.start();
        
        // 获取RingBuffer
        ringBuffer = disruptor.getRingBuffer();
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
     * 提交市价单（同步）
     * 
     * @param quantity 数量
     * @param price 价格
     * @param side 买卖方向
     * @return 订单处理结果
     * @throws Exception 处理异常
     */
    public OrderResult submitMarketOrder(int quantity, double price, Side side) throws Exception {
        CompletableFuture<OrderResult> future = new CompletableFuture<>();
        
        // 创建市价单
        MarketOrder order = new MarketOrder(marketOrderId++, quantity, price, side, 0);
        
        // 发布到Disruptor
        ringBuffer.publishEvent((event, seq) -> {
            event.setOrder(order);
            event.setMarketOrder(true);
            event.setResponseFuture(future);
        });
        
        // 阻塞等待结果
        return future.get(2, TimeUnit.SECONDS);
    }
    
    /**
     * 提交限价单（异步）
     * 
     * @param quantity 数量
     * @param price 价格 
     * @param side 买卖方向
     */
    public void submitLimitOrder(int quantity, double price, Side side) {
        // 创建限价单
        LimitOrder order = new LimitOrder(limitOrderId++, quantity, price, side);
        
        // 发布到Disruptor
        ringBuffer.publishEvent((event, seq) -> {
            event.setOrder(order);
            event.setMarketOrder(false);
            event.setResponseFuture(null);
        });
    }
    
    /**
     * 取消订单
     * 
     * @param side 买卖方向
     * @param orderId 订单ID
     */
    public void cancelOrder(Side side, int orderId) {
        ORDER_BOOK(side).onCancelOrder(orderId);
    }
    
    /**
     * 关闭撮合引擎
     */
    public void shutdown() {
        if (disruptor != null) {
            disruptor.shutdown();
        }
    }
}
