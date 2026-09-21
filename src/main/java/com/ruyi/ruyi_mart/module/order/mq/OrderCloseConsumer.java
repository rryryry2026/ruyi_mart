package com.ruyi.ruyi_mart.module.order.mq;

import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = OrderEventProducer.ORDER_CLOSE_TOPIC,
        consumerGroup = "ruyi_order_close_consumer_group"
)
public class OrderCloseConsumer implements RocketMQListener<String> {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(String orderIdStr){
        Long orderId;
        try{
            orderId = Long.parseLong(orderIdStr);
        }catch (NumberFormatException e){
            log.error("延迟关单消息格式错误，orderId 不是合法数字: {}", orderIdStr);
            return;
        }

        Order order = orderMapper.selectById(orderId);
        if(order == null){
            log.warn("延迟关单消息到达，但订单不存在 orderId={}", orderId);
            return;
        }
        // 这里不再自己判状态、也不再抢 Redis 标记：
        // 消息可能和定时任务、用户取消同时到达，只有数据库那次条件更新说了算。
        // closePendingOrder 抢不到状态流转就什么都不会做（包括不回补库存）。
        if(orderService.closePendingOrder(orderId)){
            log.info("延迟消息触发，订单 {} 已关闭并回补库存", orderId);
        }else{
            log.info("订单 {} 无需关闭（已支付/已取消/已被其它路径关闭）", orderId);
        }
    }
}
