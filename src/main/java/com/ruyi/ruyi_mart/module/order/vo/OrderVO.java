package com.ruyi.ruyi_mart.module.order.vo;

import com.ruyi.ruyi_mart.module.order.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer status;
    private LocalDateTime createTime;
    private List<OrderItem> items;
}
