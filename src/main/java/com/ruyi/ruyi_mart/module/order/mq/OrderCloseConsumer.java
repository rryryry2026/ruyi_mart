package com.ruyi.ruyi_mart.module.order.mq;

import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.enums.OrderStatus;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.order.task.OrderTimeoutCloseTask;
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
    private OrderTimeoutCloseTask orderTimeoutCloseTask;

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
        if(order.getStatus() != OrderStatus.PENDING.getCode()){
            log.info("订单 {} 已非待支付状态(status={})，跳过延迟关单", orderId, order.getStatus());
            return;
        }

        orderTimeoutCloseTask.closeOrder(order);
        log.info("延迟消息触发，订单 {} 已关闭", orderId);
    }
}
