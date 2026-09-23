package com.ruyi.ruyi_mart.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

//互斥组表 coupon_mutex_group 的镜像。券上的 mutex_group_code 指向它。
@Data
@TableName("coupon_mutex_group")
public class CouponMutexGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**互斥组编码：建券时填在券上，全表唯一*/
    private Long groupCode;

    /**组名，便于人工识别（目前由建券时自动生成）*/
    private String groupName;

    private String remark;

    private LocalDateTime createTime;
}
