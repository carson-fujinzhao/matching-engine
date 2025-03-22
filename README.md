# 交易撮合引擎

高性能的交易撮合引擎，用于实现金融交易所核心撮合功能。

## 主要特点

- 高效的价格-时间优先撮合算法
- 支持限价单（Limit Order）处理
- 订单薄（Order Book）深度管理
- 快速的订单匹配和执行
- 极低延迟的交易处理
- 基于FastUtil高性能集合框架

## 核心组件

- **Engine**: 撮合引擎核心，管理买卖订单簿
- **OrderBook**: 订单薄实现，维护价格级别和订单队列
- **PriceLevel**: 价格级别，管理同一价格的订单队列
- **LimitOrderHandler**: 限价单处理器，处理限价单的添加、撮合和取消
- **Trade**: 交易记录，包含成交价格、数量和订单ID

## 使用说明

### 依赖项

- Java 8+
- FastUtil 8.5.15+（用于高性能集合实现）

### 示例代码

```java
// 创建限价单处理器
LimitOrderHandler handler = new LimitOrderHandler();

// 添加卖单
handler.matchOrder(10, 100.0, Side.SELL);

// 添加买单并获取交易结果
List<Trade> trades = handler.matchOrder(10, 100.0, Side.BUY);

// 取消订单
handler.cancelOrder(Side.SELL, orderId);
```

## 测试案例

该项目包含全面的测试案例，覆盖了各种交易场景：

1. **基本功能测试**
   - 添加单个卖单测试
   - 简单买单匹配测试
   - 取消订单测试
   - 添加多个价格级别测试

2. **撮合逻辑测试**
   - 部分成交测试
   - 价格优先撮合测试
   - 时间优先撮合测试
   - 多笔交易测试
   - 重复价格测试

3. **特殊场景测试**
   - 大订单匹配测试
   - 极限价格匹配测试
   - 边界数量测试
   - 零数量订单测试
   - 负价格测试
   - 取消不存在订单测试

4. **高级场景测试**
   - 高频交易场景测试
   - 订单积压处理测试
   - 价格穿越场景测试
   - 高速小单处理测试
   - 市场波动场景测试

运行测试的方法：

```bash
javac -d target/test-classes -cp "target/classes:path/to/fastutil.jar" src/test/java/trader/LimitOrderHandlerTest.java
java -cp "target/classes:target/test-classes:path/to/fastutil.jar" trader.LimitOrderHandlerTest
```

## 性能优化

1. **数据结构优化**
   - 使用FastUtil的高性能映射实现价格级别索引
   - 采用数组存储订单，避免链表的内存开销

2. **算法优化**
   - 价格优先匹配确保最优价格优先成交
   - 时间优先匹配确保先来先得的公平交易

3. **内存管理**
   - 对象复用减少垃圾回收压力
   - 合理的缓存策略提高热点数据访问速度

4. **并发控制**
   - 事务性处理确保订单撮合的原子性
   - 锁优化减少线程竞争

## 未来计划

1. 添加市价单（Market Order）支持
2. 实现更多订单类型（冰山单、FOK等）
3. 增加历史订单查询功能
4. 提供REST API接口
5. 支持分布式部署
