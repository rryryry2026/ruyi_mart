package com.ruyi.ruyi_mart.module.stats.mapper;

import com.ruyi.ruyi_mart.module.stats.vo.OrderStatusCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作台统计用的聚合查询。
 *
 * 这两个指标原来是把行捞回应用里再分组、再求和 —— 平时看不出问题，
 * 但订单表一到大促就是几万行全进内存。改成交给数据库算，只回结果。
 */
@Mapper
public interface StatsMapper {

    /**各状态订单数：一条 GROUP BY 查询，有几个状态就回几行*/
    @Select("SELECT status AS status, COUNT(*) AS orderCount FROM order_info GROUP BY status")
    List<OrderStatusCountVO> countOrdersByStatus();

    /**
     * 今日销售额。
     * 口径：已支付(1)、已发货(5)、已完成(6) 三种状态计入，取值见 OrderStatus。
     * 用 CASE WHEN 在 SQL 里筛，避免把今日订单一整批捞回内存再过滤求和；
     * 今天一单没有时 SUM 返回 NULL，用 IFNULL 兜成 0。
     */
    @Select("SELECT IFNULL(SUM(CASE WHEN status IN (1, 5, 6) THEN total_amount ELSE 0 END), 0) " +
            "FROM order_info WHERE create_time >= #{start}")
    BigDecimal sumSalesAmountSince(@Param("start") LocalDateTime start);
}
