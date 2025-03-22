package core.event;

import core.bean.Order;
import java.util.concurrent.CompletableFuture;

/**
 * 撮合事件对象
 */
public class OrderMatchEvent {
    private Order order;
    private boolean isMarketOrder;
    private CompletableFuture<OrderResult> responseFuture; // 市价单同步使用

    public OrderMatchEvent() {
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public boolean isMarketOrder() {
        return isMarketOrder;
    }

    public void setMarketOrder(boolean marketOrder) {
        isMarketOrder = marketOrder;
    }

    public CompletableFuture<OrderResult> getResponseFuture() {
        return responseFuture;
    }

    public void setResponseFuture(CompletableFuture<OrderResult> responseFuture) {
        this.responseFuture = responseFuture;
    }

    public boolean isSync() {
        return responseFuture != null;
    }

    public void clear() {
        this.order = null;
        this.isMarketOrder = false;
        this.responseFuture = null;
    }
} 