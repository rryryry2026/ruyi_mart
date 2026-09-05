package com.ruyi.ruyi_mart.module.coupon.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户端核销优惠券入参（结算时使用）
 */

@Data
public class CouponUseDTO {

    /**用户券ID（关联 coupon_user.id，必填）*/
    @NotNull(message = "用户券ID不能为空")
    private Long userCouponId;

    /**订单ID（必填）*/
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**订单项ID（单品券核销用，全场券可空）*/
    private Long orderItemId;
}
