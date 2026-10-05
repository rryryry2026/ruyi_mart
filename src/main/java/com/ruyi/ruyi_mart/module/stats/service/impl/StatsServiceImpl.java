package com.ruyi.ruyi_mart.module.stats.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.enums.OrderStatus;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.refund.entity.Refund;
import com.ruyi.ruyi_mart.module.refund.enums.RefundStatus;
import com.ruyi.ruyi_mart.module.refund.mapper.RefundMapper;
import com.ruyi.ruyi_mart.module.stats.mapper.StatsMapper;
import com.ruyi.ruyi_mart.module.stats.service.StatsService;
import com.ruyi.ruyi_mart.module.stats.vo.OrderStatusCountVO;
import com.ruyi.ruyi_mart.module.stats.vo.StatsSummaryVO;
import com.ruyi.ruyi_mart.module.stock.entity.Stock;
import com.ruyi.ruyi_mart.module.stock.mapper.StockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**返回工作台六个指标。*/
@Service
public class StatsServiceImpl implements StatsService {

    /**低库存阈值：可用库存 ≤ 该值视为预警*/
    private static final int LOW_STOCK_THRESHOLD = 5;

    /**业务时区：工作台"今日"口径固定按中国时区算，不跟随部署机的 JVM 时区*/
    private static final java.time.ZoneId BUSINESS_ZONE = java.time.ZoneId.of("Asia/Shanghai");

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private RefundMapper refundMapper;
    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private StatsMapper statsMapper;

    /**
     * 工作台六个指标。
     *
     * 只统计数用 count、要汇总的交给 StatsMapper 聚合，不要把订单行捞回内存再算。
     * 加只读事务是为了让这一串查询落在同一个一致性快照上：
     * 否则六条语句各自一个快照，并发下可能出现"订单数已经变了、销售额还是旧的"这种自相矛盾的返回。
     */
    @Override
    @Transactional(readOnly = true)
    public StatsSummaryVO summary() {
        StatsSummaryVO vo = new StatsSummaryVO();
        // "今日"固定按业务所在时区（中国）算：LocalDate.now() 依赖 JVM 时区，
        // 服务器若跑在 UTC，零点会差 8 小时，"今日订单数"就对不上
        LocalDateTime todayStart = LocalDate.now(BUSINESS_ZONE).atStartOfDay();

        //今日订单数（含未支付）
        vo.setTodayOrderCount(orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStart)));

        //今日销售额：已支付/已发货/已完成计入，数据库里聚合完只回一个数
        vo.setTodaySalesAmount(statsMapper.sumSalesAmountSince(todayStart));

        //待发货数
        vo.setPendingShipCount(orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PAID.getCode())));

        //待退款审核数
        vo.setPendingRefundCount(refundMapper.selectCount(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getStatus, RefundStatus.PENDING.getCode())));

        //低库存商品数（只统计已初始化库存的商品，没建库存记录的不算"低库存"）
        vo.setLowStockCount(stockMapper.selectCount(new LambdaQueryWrapper<Stock>()
                .le(Stock::getAvailable, LOW_STOCK_THRESHOLD)));

        //各状态订单数分布：数据库 GROUP BY，回几行就是几个状态
        Map<Integer, Long> counts = statsMapper.countOrdersByStatus().stream()
                .filter(c -> c.getStatus() != null)
                .collect(Collectors.toMap(OrderStatusCountVO::getStatus, OrderStatusCountVO::getOrderCount));
        vo.setOrderStatusCounts(counts);

        return vo;
    }
}
