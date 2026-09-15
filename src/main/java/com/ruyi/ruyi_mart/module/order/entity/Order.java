package com.ruyi.ruyi_mart.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_info")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer status;

    /**
     * 收货地址快照：下单时从地址表复制过来，之后不随地址改动而变。
     * 存快照而不是只存 address_id —— 地址可能被用户改掉或删除，
     * 历史订单必须保留下单当时的收货信息，否则商家发货时看到的是新地址。
     */
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;

    @TableField("detail_address")
    private String detailAddress;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

