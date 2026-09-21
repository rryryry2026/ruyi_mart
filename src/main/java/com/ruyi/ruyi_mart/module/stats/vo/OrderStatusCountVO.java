package com.ruyi.ruyi_mart.module.stats.vo;

import lombok.Data;

/**各状态订单数：GROUP BY status 查询回来的一行。*/
@Data
public class OrderStatusCountVO {

    /**订单状态码，取值见 OrderStatus*/
    private Integer status;

    /**该状态下的订单数*/
    private Long orderCount;
}
