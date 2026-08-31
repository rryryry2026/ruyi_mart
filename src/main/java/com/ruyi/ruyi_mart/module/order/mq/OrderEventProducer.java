package com.ruyi.ruyi_mart.module.order.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    public static final String ORDER_CLOSE_TOPIC = "ruyi_order_close_delay";

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${ruyi-mart.order.close-delay-level:16}")
    private int closeDelayLevel;

    public void sendCloseDelay(Long orderId) {
        Message<String> msg = MessageBuilder.withPayload(String.valueOf(orderId))
                .setHeader(RocketMQHeaders.KEYS, String.valueOf(orderId))
                .build();
        rocketMQTemplate.syncSend(ORDER_CLOSE_TOPIC, msg, 3000, closeDelayLevel);
    }
}
