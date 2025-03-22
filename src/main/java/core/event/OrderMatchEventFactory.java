package core.event;

import com.lmax.disruptor.EventFactory;

/**
 * 撮合事件工厂
 */
public class OrderMatchEventFactory implements EventFactory<OrderMatchEvent> {
    @Override
    public OrderMatchEvent newInstance() {
        return new OrderMatchEvent();
    }
} 