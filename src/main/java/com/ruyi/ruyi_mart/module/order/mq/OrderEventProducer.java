package com.ruyi.ruyi_mart.module.order.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendOrderCreated(Long orderId){
        rocketMQTemplate.convertAndSend("order-created",orderId,message -> {
            MessageHeaderAccessor accessor = MessageHeaderAccessor.getMutableAccessor(message);
            accessor.setHeader(RocketMQHeaders.KEYS, String.valueOf(orderId));
            return MessageBuilder.createMessage(message.getPayload(),accessor.getMessageHeaders());
        });
    }
}
