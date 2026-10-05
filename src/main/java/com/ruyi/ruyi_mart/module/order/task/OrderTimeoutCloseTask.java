package com.ruyi.ruyi_mart.module.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.enums.OrderStatus;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时未支付订单的兜底扫描。
 *
 * 这里只负责"找出超时单"，真正的关单 + 回补库存交给 OrderService.closePendingOrder()：
 * 一、跨类调用才能走 Spring 代理，事务才生效。这个类里直接写 @Transactional 方法
 *     再自己调用自己（closeExpiredOrders 调 closeOrder）是拿不到事务的
 *     —— 自调用不经过代理，回补库存和改状态会各自独立提交，
 *     中间失败一次就会出现"库存已补、订单还开着"，下一轮扫描再补一遍。
 * 二、关单必须以"抢到状态流转"为准（见 OrderMapper.changeStatusIf），
 *     而不是靠这里的判断或一个 Redis 标记：用户取消、支付回调、
 *     另一个实例的定时任务都可能同时动同一笔订单。
 */
@Slf4j
@Component
public class OrderTimeoutCloseTask {

    /** 订单待支付超时时长（分钟） */
    private static final int TIMEOUT_MINUTES = 30;

    /** 单轮最多处理的订单数：积压时一次性全捞进内存会拖垮应用，剩下的留给下一轮（60 秒后） */
    private static final int BATCH_LIMIT = 500;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderService orderService;

    @Scheduled(fixedDelay = 60_000)
    public void closeExpiredOrders(){
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        // 状态用枚举常量而不是魔法数字 0：OrderStatus.PENDING 改码时这里才不会悄悄失配
        qw.eq(Order::getStatus, OrderStatus.PENDING.getCode()).lt(Order::getCreateTime,deadline);
        qw.last("LIMIT " + BATCH_LIMIT);
        List<Order> expired = orderMapper.selectList(qw);
        if(expired.isEmpty()){
            return;
        }
        log.info("扫描到 {} 笔超时未支付订单，开始关闭",expired.size());
        for(Order order : expired){
            try{
                orderService.closePendingOrder(order.getId());
            }catch (Exception e){
                // 单笔失败不能影响这一批里其它订单
                log.error("关闭超时订单失败 orderId={}", order.getId(), e);
            }
        }
    }
}
