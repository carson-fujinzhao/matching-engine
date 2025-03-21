package core.order;

import core.EngineConstants;
import core.bean.Order;
import core.bean.Side;
import core.bean.Trade;
import core.price.PriceLevel;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;

import java.util.*;

public class OrderBook implements EngineConstants {
    final Side side;
    // 使用TreeMap按价格排序的价格级别，买单为降序，卖单为升序
    final NavigableMap<Double, PriceLevel> priceLevels;
    Object2ObjectRBTreeMap<Double, PriceLevel> priceLevels2 = new Object2ObjectRBTreeMap<>();
    
    // 订单索引
    final Map<Integer, OrderInfo> orderIndex = new HashMap<>();

    public OrderBook(Side side) {
        this.side = side;
        // 买单簿按价格降序排列，卖单簿按价格升序排列
        this.priceLevels = side == Side.BUY 
            ? new TreeMap<>(Collections.reverseOrder()) 
            : new TreeMap<>();
    }

    // 添加getter方法获取价格级别映射
    public NavigableMap<Double, PriceLevel> getPriceLevels() {
        return priceLevels;
    }

    public List<Trade> matchOrder(Order order) {
        if (side.equals(order.getSide())) {
            return Collections.emptyList();
        }

        final List<Trade> trades = new ArrayList<>();
        
        // 买单匹配卖单
        Iterator<Map.Entry<Double, PriceLevel>> iterator = priceLevels.entrySet().iterator();
        if (order.getSide() == Side.BUY) {
            // 遍历所有卖单，从最低价开始
            while (iterator.hasNext() && !order.isCompleted()) {
                Map.Entry<Double, PriceLevel> entry = iterator.next();
                double price = entry.getKey();
                
                // 如果最低卖价高于买价，无法匹配
                if (price > order.getPrice()) {
                    break;
                }
                
                PriceLevel priceLevel = entry.getValue();
                priceLevel.matchOrder(order, trades);
                
                // 如果价格级别为空，移除该价格级别
                if (priceLevel.isEmpty()) {
                    iterator.remove();
                }
            }
        } 
        // 卖单匹配买单
        else {
            // 遍历所有买单，从最高价开始
            while (iterator.hasNext() && !order.isCompleted()) {
                Map.Entry<Double, PriceLevel> entry = iterator.next();
                double price = entry.getKey();
                
                // 如果最高买价低于卖价，无法匹配
                if (price < order.getPrice()) {
                    break;
                }
                
                PriceLevel priceLevel = entry.getValue();
                priceLevel.matchOrder(order, trades);
                
                // 如果价格级别为空，移除该价格级别
                if (priceLevel.isEmpty()) {
                    iterator.remove();
                }
            }
        }

        return trades;
    }

    public void onNewOrder(Order order) {
        final double price = order.getPrice();
        
        // 获取或创建价格级别
        PriceLevel priceLevel = priceLevels.computeIfAbsent(price, p -> new PriceLevel(side));
        
        // 添加订单到价格级别
        int orderPosition = priceLevel.newOrder(order);
        
        // 记录订单信息
        orderIndex.put(order.getOrderId(), new OrderInfo(price, orderPosition));
    }

    public void onCancelOrder(int orderId) {
        OrderInfo info = orderIndex.get(orderId);
        if (info == null) {
            return;
        }
        
        PriceLevel priceLevel = priceLevels.get(info.price);
        if (priceLevel != null) {
            priceLevel.cancelOrder(info.position);
            
            // 如果价格级别为空，移除该价格级别
            if (priceLevel.isEmpty()) {
                priceLevels.remove(info.price);
            }
        }
        
        orderIndex.remove(orderId);
    }
    
    // 订单信息类
    private static class OrderInfo {
        final double price;
        final int position;
        
        public OrderInfo(double price, int position) {
            this.price = price;
            this.position = position;
        }
    }
}
