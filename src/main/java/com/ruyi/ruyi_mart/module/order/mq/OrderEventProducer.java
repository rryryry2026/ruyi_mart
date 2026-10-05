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
        // 同步发送的超时压到 1 秒：它是下单链路里的一段阻塞调用，
        // MQ 抖动时不能让用户等 3 秒。发送失败由调用方 catch 后走定时任务兜底关单，
        // 所以这里"快失败"比"重试到成功"更合适
        rocketMQTemplate.syncSend(ORDER_CLOSE_TOPIC, msg, 1000, closeDelayLevel);
    }
}
