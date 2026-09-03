package com.ruyi.ruyi_mart.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coupon_scope_detail")
public class CouponScopeDetail {

    /**主键*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /**优惠券ID*/
    @TableField("coupon_id")
    private Long couponId;

    /**作用对象类型：1商品 2分类*/
    @TableField("scope_type")
    private Integer scopeType;

    /**作用对象ID：商品ID或分类ID*/
    @TableField("target_id")
    private Long targetId;

    /**创建时间*/
    @TableField("create_time")
    private LocalDateTime createTime;
}
