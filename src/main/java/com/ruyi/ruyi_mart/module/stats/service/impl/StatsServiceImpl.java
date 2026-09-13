package com.ruyi.ruyi_mart.module.stats.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.enums.OrderStatus;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.refund.entity.Refund;
import com.ruyi.ruyi_mart.module.refund.enums.RefundStatus;
import com.ruyi.ruyi_mart.module.refund.mapper.RefundMapper;
import com.ruyi.ruyi_mart.module.stock.entity.Stock;
import com.ruyi.ruyi_mart.module.stock.mapper.StockMapper;
import com.ruyi.ruyi_mart.module.stats.service.StatsService;
import com.ruyi.ruyi_mart.module.stats.vo.StatsSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatsServiceImpl implements StatsService {

    /**低库存阈值：可用库存 ≤ 该值视为预警*/
    private static final int LOW_STOCK_THRESHOLD = 5;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private RefundMapper refundMapper;
    @Autowired
    private StockMapper stockMapper;

    @Override
    public StatsSummaryVO summary() {
        StatsSummaryVO vo = new StatsSummaryVO();

        // 今日订单（含未支付）与销售额：一次性取今日订单，内存里分别统计
        List<Order> todayOrders = orderMapper.selectList(
                new QueryWrapper<Order>().ge("create_time", LocalDate.now().atStartOfDay()));
        vo.setTodayOrderCount((long) todayOrders.size());
        List<Integer> paidStatuses = Arrays.asList(
                OrderStatus.PAID.getCode(),
                OrderStatus.SHIPPED.getCode(),
                OrderStatus.COMPLETED.getCode());
        vo.setTodaySalesAmount(todayOrders.stream()
                .filter(o -> o.getTotalAmount() != null && paidStatuses.contains(o.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        vo.setPendingShipCount(orderMapper.selectCount(
                new QueryWrapper<Order>().eq("status", OrderStatus.PAID.getCode())));

        vo.setPendingRefundCount(refundMapper.selectCount(
                new QueryWrapper<Refund>().eq("status", RefundStatus.PENDING.getCode())));

        vo.setLowStockCount(stockMapper.selectCount(
                new QueryWrapper<Stock>().le("available", LOW_STOCK_THRESHOLD)));

        // 各状态订单数分布（只查 status 列）
        List<Order> statusOnly = orderMapper.selectList(
                new QueryWrapper<Order>().select("status"));
        Map<Integer, Long> counts = statusOnly.stream()
                .filter(o -> o.getStatus() != null)
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        vo.setOrderStatusCounts(counts);
        return vo;
    }
}
