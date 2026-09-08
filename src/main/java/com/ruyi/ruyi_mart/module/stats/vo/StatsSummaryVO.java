package com.ruyi.ruyi_mart.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 工作台概览统计
 */
@Data
public class StatsSummaryVO {

    /**今日订单数（含未支付）*/
    private Long todayOrderCount;

    /**今日销售额（实付口径：已支付/已发货/已完成）*/
    private BigDecimal todaySalesAmount;

    /**待发货订单数（已支付未发货）*/
    private Long pendingShipCount;

    /**待审核退款数*/
    private Long pendingRefundCount;

    /**低库存商品数（可用 ≤ 5）*/
    private Long lowStockCount;

    /**各订单状态数量分布，key 为 OrderStatus code*/
    private Map<Integer, Long> orderStatusCounts;
}
