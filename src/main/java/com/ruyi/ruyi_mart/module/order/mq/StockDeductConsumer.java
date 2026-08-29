package com.ruyi.ruyi_mart.module.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.entity.OrderItem;
import com.ruyi.ruyi_mart.module.order.enums.OrderStatus;
import com.ruyi.ruyi_mart.module.order.mapper.OrderItemMapper;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RocketMQMessageListener(
        topic = "order-created",
        consumerGroup = "stock_deduct_group"
)
public class StockDeductConsumer implements RocketMQListener<Long> {

    private static final Logger log = LoggerFactory.getLogger(StockDeductConsumer.class);

    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void onMessage(Long orderId){
        String idemKey = "ruyi:mq:stock-deduct:" + orderId;
        RBucket<String> bucket = redissonClient.getBucket(idemKey);

        String status = bucket.get();
        if("SUCCESS".equals(status)){
            log.info("订单 {} 扣库存已完成(SUCCESS)，幂等跳过", orderId);
            return;
        }
        if("FAILED".equals(status)){
            log.info("订单 {} 扣库存已记录失败(FAILED)，跳过", orderId);
            return;
        }

        boolean locked = bucket.setIfAbsent("PROCESSING",Duration.ofMinutes(10));
        if(!locked && !"PROCESSING".equals(bucket.get())){
            log.info("订单 {} 被其他处理者占用，跳过", orderId);
            return;
        }


        try {
            QueryWrapper<OrderItem> qw = new QueryWrapper<>();
            qw.eq("order_id",orderId);
            var items = orderItemMapper.selectList(qw);
            if(items == null || items.isEmpty()) {
                log.warn("订单 {} 无明细，跳过", orderId);
                bucket.set("FAILED", Duration.ofMinutes(10));
                return;
            }

            boolean allOk = true;
            List<OrderItem> deducted = new ArrayList<>();
            for(OrderItem item : items){
                int rows = productService.deductStock(item.getProductId(),item.getQuantity());
                if(rows == 0){
                    allOk = false;
                    log.error("扣库存失败：商品 {} 库存不足，订单 {} 标记失败", item.getProductId(), orderId);
                    for(OrderItem done : deducted){
                        productService.addStock(done.getProductId(),done.getQuantity());
                    }
                    break;
                }
                deducted.add(item);
            }

            Order order = new Order();
            order.setId(orderId);
            order.setStatus(allOk ? OrderStatus.PENDING.getCode() : OrderStatus.STOCK_FAILED.getCode());
            orderMapper.updateById(order);

            bucket.set(allOk ? "SUCCESS" : "FAILED",Duration.ofMinutes(10));
            log.info("订单 {} 扣库存完成，结果={}", orderId, allOk ? "成功" : "库存不足");

        } catch (Exception e){

            bucket.set("FAILED",Duration.ofMinutes(10));
            log.error("订单 {} 扣库存异常", orderId, e);
        }

    }
}
