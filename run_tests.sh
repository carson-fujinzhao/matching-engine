#!/bin/bash

# 创建目录
mkdir -p target/classes target/test-classes

# 准备classpath
LIB_CLASSPATH="./lib/disruptor-3.4.4.jar:./lib/fastutil-8.5.12.jar"

# 编译源代码
echo "编译源代码..."
javac -cp "$LIB_CLASSPATH" -d target/classes src/main/java/core/bean/Side.java src/main/java/core/bean/Order.java src/main/java/core/bean/Trade.java src/main/java/core/order/OrderBook.java src/main/java/core/price/PriceLevel.java src/main/java/trader/bean/LimitOrder.java src/main/java/core/EngineConstants.java src/main/java/core/Engine.java src/main/java/core/event/OrderMatchEvent.java src/main/java/core/event/OrderMatchEventFactory.java src/main/java/core/event/OrderMatchEventHandler.java src/main/java/core/event/OrderResult.java src/main/java/trader/TradePersistence.java src/main/java/trader/bean/MarketOrder.java

# 编译测试代码
echo "编译测试代码..."
javac -cp "target/classes:$LIB_CLASSPATH" -d target/test-classes src/test/java/core/EngineDirectTest.java

# 如果编译成功则运行测试
if [ $? -eq 0 ]; then
    echo "运行测试..."
    java -cp "target/classes:target/test-classes:$LIB_CLASSPATH" core.EngineDirectTest
else
    echo "编译失败，无法运行测试"
    exit 1
fi 