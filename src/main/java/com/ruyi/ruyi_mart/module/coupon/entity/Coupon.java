package com.ruyi.ruyi_mart.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponTypeEnum;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponValidModeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
public class Coupon {

    /**主键*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /**优惠券模板编码*/
    @TableField("coupon_no")
    private String couponNo;

    /**活动名称*/
    @TableField("activity_name")
    private String activityName;

    /**券类型：1满减 2折扣 3无门槛 4单品券（枚举，存数字）*/
    @TableField("coupon_type")
    private CouponTypeEnum couponType;

    /**满减/无门槛面额*/
    @TableField("face_value")
    private BigDecimal faceValue;

    /**折扣率，8.8 表示 88 折*/
    @TableField("discount_rate")
    private BigDecimal discountRate;

    /**折扣上限*/
    @TableField("max_discount")
    private BigDecimal maxDiscount;

    /**使用门槛（满多少可用）*/
    @TableField("min_spend")
    private BigDecimal minSpend;

    /**总发行量，0 表示不限*/
    @TableField("total_quota")
    private Integer totalQuota;

    /**已核销数*/
    @TableField("used_quota")
    private Integer usedQuota;

    /**已领取数*/
    @TableField("receive_quota")
    private Integer receiveQuota;

    /**有效期模式：1固定时间 2领券后N天（枚举，存数字）*/
    @TableField("valid_mode")
    private CouponValidModeEnum validMode;

    /**固定有效期开始*/
    @TableField("valid_start")
    private LocalDateTime validStart;

    /**固定有效期结束*/
    @TableField("valid_end")
    private LocalDateTime validEnd;

    /**领券后有效天数*/
    @TableField("receive_valid_days")
    private Integer receiveValidDays;

    /**单人限领*/
    @TableField("limit_per_person")
    private Integer limitPerPerson;

    /**适用人群：1全部 2新人 3会员 4指定人群*/
    @TableField("user_limit_type")
    private Integer userLimitType;

    /**使用范围：1全场 2指定商品 3指定分类*/
    @TableField("use_scope")
    private Integer useScope;

    /**互斥组唯一编码，0 表示无*/
    @TableField("mutex_group_code")
    private Long mutexGroupCode;

    /**模板状态：0未开始 1发放中 2已结束 3作废*/
    @TableField("status")
    private Integer status;

    /**是否隐藏：0否 1是*/
    @TableField("is_elimination")
    private Integer isElimination;

    /**发行时间*/
    @TableField("release_time")
    private LocalDateTime releaseTime;

    /**创建时间*/
    @TableField("create_time")
    private LocalDateTime createTime;

    /**更新时间*/
    @TableField("update_time")
    private LocalDateTime updateTime;
}
