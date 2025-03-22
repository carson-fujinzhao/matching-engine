package core;

import core.bean.Side;
import core.event.OrderResult;

/**
 * 撮合引擎示例
 */
public class EngineDemo {
    public static void main(String[] args) {
        try {
            // 创建引擎
            Engine engine = new Engine();
            
            System.out.println("===== 演示市价单和限价单撮合 =====");
            
            // 先提交一些限价单，建立委托簿
            System.out.println("提交卖出限价单 (限价100, 数量5)");
            engine.submitLimitOrder(5, 100, Side.SELL);
            
            System.out.println("提交卖出限价单 (限价102, 数量3)");
            engine.submitLimitOrder(3, 102, Side.SELL);
            
            System.out.println("提交买入限价单 (限价98, 数量10)");
            engine.submitLimitOrder(10, 98, Side.BUY);
            
            // 等待异步处理完成
            Thread.sleep(500);
            
            System.out.println("\n===== 提交市价单（同步处理） =====");
            
            // 提交市价单，应该会与卖单成交
            System.out.println("提交买入市价单 (价格101, 数量4)...");
            OrderResult result = engine.submitMarketOrder(4, 101, Side.BUY);
            System.out.println("市价单结果: " + result.getMessage() + ", 订单ID: " + result.getOrderId());
            
            // 再提交一个限价单
            System.out.println("\n提交新的买入限价单 (限价103, 数量6)");
            engine.submitLimitOrder(6, 103, Side.BUY);
            
            // 等待异步处理完成
            Thread.sleep(500);
            
            // 关闭引擎
            System.out.println("\n关闭撮合引擎");
            engine.shutdown();
            
        } catch (Exception e) {
            System.err.println("运行过程中出现异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 