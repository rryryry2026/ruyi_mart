package com.ruyi.ruyi_mart.module.order.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.entity.OrderItem;
import com.ruyi.ruyi_mart.module.order.mapper.OrderItemMapper;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTimeoutCloseTask {

    /** 订单待支付超时时长（分钟） */
    private static final int TIMEOUT_MINUTES = 30;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductService productService;
    @Autowired
    private RedissonClient redissonClient;

    @Scheduled(fixedDelay = 60_000)
    public void closeExpiredOrders(){
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        QueryWrapper<Order> qw = new QueryWrapper<>();
        qw.eq("status",0).lt("create_time",deadline);
        List<Order> expired = orderMapper.selectList((qw));
        if(expired.isEmpty()){
            return;
        }
        log.info("扫描到 {} 笔超时未支付订单，开始关闭",expired.size());
        for(Order order : expired){
            closeOrder(order);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeOrder(Order order){
        Long orderId = order.getId();
        RBucket<String> bucket = redissonClient.getBucket("ruyi:mq:stock-deduct:" + orderId, StringCodec.INSTANCE);

        if(!"SUCCESS".equals(bucket.get())){
            order.setStatus(3);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            log.info("订单 {} 超时关闭（库存未锁定），直接关单", orderId);
            return;
        }
        QueryWrapper<OrderItem> itemQw = new QueryWrapper<>();
        itemQw.eq("order_id",orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemQw);
        for(OrderItem item:items){
            productService.addStock(item.getProductId(),item.getQuantity());
        }

        order.setStatus(3);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        bucket.set("CLOSED", Duration.ofMinutes(10));
        log.info("订单 {} 超时关闭，已回补锁定库存", orderId);

    }
}
