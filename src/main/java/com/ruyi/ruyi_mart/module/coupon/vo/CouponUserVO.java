package com.ruyi.ruyi_mart.module.coupon.vo;

import com.ruyi.ruyi_mart.module.coupon.enums.CouponTypeEnum;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponUseStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户券（券包 / 结算可用券）。
 *
 * 之前接口直接返回 CouponUser 实体，只有券ID、有效期、状态，
 * 前端拿不到面额和门槛，显示不出"满100减20"。
 * 这里把券模板的展示字段一并带上。
 * 类型与状态沿用枚举的 @JsonValue，序列化成英文标识（unused / fullReduction 等）。
 */
@Data
public class CouponUserVO {

    // ============ coupon_user ============
    /** 用户券ID，结算时传的 userCouponId 就是它 */
    private Long id;

    private Long couponId;

    /** 使用状态：unBegin / unused / used / expired / returned */
    private CouponUseStatusEnum useStatus;

    private LocalDateTime validStart;

    private LocalDateTime validEnd;

    private LocalDateTime createTime;

    // ============ 券模板（展示用） ============
    private String activityName;

    /** 券类型：fullReduction 满减 / discount 折扣 / noThreshold 无门槛 / item 单品 */
    private CouponTypeEnum couponType;

    /** 满减与无门槛券的面额 */
    private BigDecimal faceValue;

    /** 折扣率，8.8 表示 88 折 */
    private BigDecimal discountRate;

    /** 折扣券的抵扣上限 */
    private BigDecimal maxDiscount;

    /** 使用门槛（满多少可用） */
    private BigDecimal minSpend;

    /** 使用范围：1全场 2指定商品 3指定分类 */
    private Integer useScope;

    /**
     * 本券在给定订单金额下可抵扣的金额。
     * 只有 /coupon/available 传了 orderAmount 时才会计算，其余场景为 null。
     */
    private BigDecimal discountAmount;
}
