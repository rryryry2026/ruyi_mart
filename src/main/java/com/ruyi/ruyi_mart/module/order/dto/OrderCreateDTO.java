package com.ruyi.ruyi_mart.module.order.dto;

import lombok.Data;

@Data
public class OrderCreateDTO {

    /**用户券ID（可选，不传表示不使用优惠券）*/
    private Long userCouponId;
}
