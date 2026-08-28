package com.ruyi.ruyi_mart.module.order.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendOrderCreated(Long orderId){
        rocketMQTemplate.convertAndSend("order-created", orderId);
    }
}
