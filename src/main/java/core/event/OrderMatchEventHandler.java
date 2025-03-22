package core.event;

import com.lmax.disruptor.EventHandler;
import core.Engine;
import core.bean.Side;
import core.bean.Trade;
import trader.TradePersistence;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 撮合处理器
 */
public class OrderMatchEventHandler implements EventHandler<OrderMatchEvent> {
    private final TradePersistence tradePersistence;
    
    // 用于幂等性检查的已处理订单集合
    private final Map<Integer, Boolean> processedOrders = new ConcurrentHashMap<>();
    
    // 添加一个简单的内存缓存，记录最近处理过的订单结果，以便重复请求可以直接返回
    private final Map<Integer, OrderResult> orderResultCache = new ConcurrentHashMap<>();
    
    // 订单结果缓存大小限制
    private static final int MAX_CACHE_SIZE = 10000;

    public OrderMatchEventHandler(TradePersistence tradePersistence) {
        this.tradePersistence = tradePersistence;
    }

    @Override
    public void onEvent(OrderMatchEvent event, long sequence, boolean endOfBatch) {
        try {
            if (event.getOrder() == null) {
                return;
            }
            
            int orderId = event.getOrder().getOrderId();
            
            // 幂等性检查 - 如果订单已处理且结果已缓存，直接返回缓存结果
            if (processedOrders.containsKey(orderId) && event.isSync()) {
                OrderResult cachedResult = orderResultCache.get(orderId);
                if (cachedResult != null) {
                    event.getResponseFuture().complete(cachedResult);
                    return;
                }
            }
            
            // 标记订单为已处理
            processedOrders.put(orderId, true);
            
            // 控制缓存大小
            if (processedOrders.size() > MAX_CACHE_SIZE) {
                // 简单的缓存清理策略 - 实际应用中可能需要更复杂的LRU或过期清理
                processedOrders.clear();
                orderResultCache.clear();
            }
            
            // 执行撮合
            List<Trade> trades = Engine.OPPOSITE_ORDER_BOOK(event.getOrder().getSide()).matchOrder(event.getOrder());
            tradePersistence.addTrades(trades);

            if (!event.getOrder().isCompleted() && !event.isMarketOrder()) {
                // 只有限价单才需要放入委托簿
                Engine.ORDER_BOOK(event.getOrder().getSide()).onNewOrder(event.getOrder());
            }

            // 构造结果
            OrderResult result = new OrderResult(
                    orderId,
                    true,
                    "撮合成功，成交" + trades.size() + "笔"
            );
            
            // 缓存订单结果
            orderResultCache.put(orderId, result);

            if (event.isSync()) {
                // 市价单同步返回结果
                event.getResponseFuture().complete(result);
            } else {
                // 异步限价单处理完成，记录日志等
                System.out.println("限价单处理完毕: " + orderId);
            }
        } catch (Exception e) {
            if (event.isSync()) {
                event.getResponseFuture().completeExceptionally(e);
            } else {
                System.err.println("异步订单处理失败: " + event.getOrder().getOrderId());
                e.printStackTrace();
            }
        } finally {
            event.clear();
        }
    }
} 