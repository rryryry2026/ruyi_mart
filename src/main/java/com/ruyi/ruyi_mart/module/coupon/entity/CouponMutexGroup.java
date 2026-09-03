package com.ruyi.ruyi_mart.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coupon_mutex_group")
public class CouponMutexGroup {

    /**主键*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /**互斥组名称*/
    @TableField("group_name")
    private String groupName;

    /**互斥组唯一编码*/
    @TableField("group_code")
    private Long groupCode;

    /**备注*/
    @TableField("remark")
    private String remark;

    /**创建时间*/
    @TableField("create_time")
    private LocalDateTime createTime;
}
