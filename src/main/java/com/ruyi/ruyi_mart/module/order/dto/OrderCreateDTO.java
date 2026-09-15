package com.ruyi.ruyi_mart.module.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateDTO {

    /**
     * 收货地址ID（必填）。
     * 后端会校验地址归属，并把地址内容快照进订单，
     * 这样地址之后被改或被删，历史订单仍保留当时的收货信息。
     */
    @NotNull(message = "请选择收货地址")
    private Long addressId;

    /**用户券ID（可选，不传表示不使用优惠券）*/
    private Long userCouponId;
}
