package core.event;

/**
 * 撮合结果对象
 */
public class OrderResult {
    private int orderId;
    private boolean success;
    private String message;

    public OrderResult() {
    }

    public OrderResult(int orderId, boolean success, String message) {
        this.orderId = orderId;
        this.success = success;
        this.message = message;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 