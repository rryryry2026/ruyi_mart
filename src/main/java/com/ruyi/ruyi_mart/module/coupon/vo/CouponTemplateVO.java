package com.ruyi.ruyi_mart.module.coupon.vo;

import com.ruyi.ruyi_mart.module.coupon.enums.CouponTypeEnum;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponValidModeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领券中心的可领取券。
 *
 * 只返回"当前用户确实还能领"的券——服务端已经按领取上限、库存、互斥组先滤过一遍，
 * 避免前端展示出一个点下去必然失败的券。
 */
@Data
public class CouponTemplateVO {

    private Long id;

    private String couponNo;

    private String activityName;

    /** 券类型：fullReduction / discount / noThreshold / item */
    private CouponTypeEnum couponType;

    private BigDecimal faceValue;

    private BigDecimal discountRate;

    private BigDecimal maxDiscount;

    private BigDecimal minSpend;

    /** 有效期模式：fixedTime 固定时间 / receiveDays 领券后N天 */
    private CouponValidModeEnum validMode;

    private LocalDateTime validStart;

    private LocalDateTime validEnd;

    /** 领券后有效天数（validMode=receiveDays 时有意义） */
    private Integer receiveValidDays;

    /** 单人限领张数 */
    private Integer limitPerPerson;

    /** 已领取总量 */
    private Integer receiveQuota;

    /** 发行总量，0 表示不限量 */
    private Integer totalQuota;

    private Integer useScope;

    // ============ 当前用户维度的状态 ============
    /** 当前用户已持有该券的张数 */
    private Integer ownedCount;

    /** 当前用户还能再领几张 */
    private Integer remainToReceive;

    /** 剩余可领数量；totalQuota=0（不限量）时为 null */
    private Integer remainQuota;
}
