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

    /**
     * 收货地址（下单时的快照）。
     * 消费端订单详情用它展示"寄往哪里"，管理端发货也依赖这几个字段
     * （OrderAdminVO 继承本类，所以管理端自动带上）。
     */
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
}
