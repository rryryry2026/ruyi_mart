package com.ruyi.ruyi_mart.module.coupon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 用户端领券入参
 */

@Data
public class CouponReceiveDTO {

    /**要领取的优惠券模板ID（必填）*/
    @NotNull(message = "优惠券ID不能为空")
    private Long couponId;
}
