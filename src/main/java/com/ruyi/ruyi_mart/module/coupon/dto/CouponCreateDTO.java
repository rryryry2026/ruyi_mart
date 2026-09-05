package com.ruyi.ruyi_mart.module.coupon.dto;

import com.ruyi.ruyi_mart.module.coupon.enums.CouponTypeEnum;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponValidModeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 管理端创建优惠券入参
 */

@Data
public class CouponCreateDTO {

    /**活动名称*/
    private String activityName;

    /**券类型（枚举：满减/折扣/无门槛/单品）*/
    @NotNull(message = "券类型不能为空")
    private CouponTypeEnum couponType;

    /**满减/无门槛面额（折扣券可空）*/
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal faceValue;

    /**折扣率，8.8=88折（满减/无门槛券可空）*/
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal discountRate;

    /**折扣上限（折扣券可选）*/
    private BigDecimal maxDiscount;

    /**使用门槛（满多少可用，无门槛券为0）*/
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal minSpend;

    /**总发行量，0=不限*/
    @Min(0)
    private Integer totalQuota;

    /**有效期模式（枚举：固定时间/领券后N天）*/
    @NotNull(message = "有效期模式不能为空")
    private CouponValidModeEnum validMode;

    /**固定有效期开始（validMode=固定时间时必填）*/
    private LocalDateTime validStart;

    /**固定有效期结束（validMode=固定时间时必填）*/
    private LocalDateTime validEnd;

    /**领券后有效天数（validMode=领券后N天时必填）*/
    @Min(0)
    private Integer receiveValidDays;

    /**单人限领，默认1*/
    @Min(1)
    private Integer limitPerPerson;

    /**适用人群：1全部 2新人 3会员 4指定人群*/
    private Integer userLimitType;

    /**使用范围：1全场 2指定商品 3指定分类*/
    private Integer useScope;

    /**互斥组唯一编码，0=无*/
    private Long mutexGroupCode;

    /**模板状态：0未开始 1发放中 2已结束 3作废*/
    private Integer status;
}
