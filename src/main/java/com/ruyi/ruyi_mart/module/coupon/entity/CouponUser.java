package com.ruyi.ruyi_mart.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponUseStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coupon_user")
public class CouponUser {

    /**主键*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /**用户ID*/
    @TableField("user_id")
    private Long userId;

    /**优惠券模板ID*/
    @TableField("coupon_id")
    private Long couponId;

    /**个人生效时间*/
    @TableField("valid_start")
    private LocalDateTime validStart;

    /**个人过期时间*/
    @TableField("valid_end")
    private LocalDateTime validEnd;

    /**使用状态：0未开始 1未用 2已用 3已过期 4已退回（枚举，存数字）*/
    @TableField("use_status")
    private CouponUseStatusEnum useStatus;

    /**领券时间*/
    @TableField("create_time")
    private LocalDateTime createTime;

    /**更新时间*/
    @TableField("update_time")
    private LocalDateTime updateTime;
}
