package com.ruyi.ruyi_mart.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_order_rel")
public class CouponOrderRel {

    /**主键*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /**订单ID*/
    @TableField("order_id")
    private Long orderId;

    /**订单项ID（单品券核销用，全场券为0）*/
    @TableField("order_item_id")
    private Long orderItemId;

    /**用户券ID（关联 coupon_user.id）*/
    @TableField("user_coupon_id")
    private Long userCouponId;

    /**活动ID*/
    @TableField("activity_id")
    private Long activityId;

    /**本次抵扣金额*/
    @TableField("discount_amount")
    private BigDecimal discountAmount;

    /**核销状态：1正常核销 2退款回滚 3作废*/
    @TableField("rel_status")
    private Integer relStatus;

    /**核销时间*/
    @TableField("use_time")
    private LocalDateTime useTime;

    /**退款回滚时间*/
    @TableField("refund_time")
    private LocalDateTime refundTime;

    /**创建时间*/
    @TableField("create_time")
    private LocalDateTime createTime;
}
