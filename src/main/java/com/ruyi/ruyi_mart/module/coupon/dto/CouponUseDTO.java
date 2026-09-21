package com.ruyi.ruyi_mart.module.coupon.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;


/**核销优惠券的入参。*/
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

    /**
     * 订单金额（必填）。
     * 不能省：满减券的门槛校验依赖它，缺了会退化成"直接按面额抵扣"，
     * 等于"满 800 减 100"的券可以无门槛使用。
     */
    @NotNull(message = "订单金额不能为空")
    private BigDecimal orderAmount;
}
