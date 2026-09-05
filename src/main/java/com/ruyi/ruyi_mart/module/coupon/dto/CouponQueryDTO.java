package com.ruyi.ruyi_mart.module.coupon.dto;

import com.ruyi.ruyi_mart.module.coupon.enums.CouponTypeEnum;
import lombok.Data;


/**
 * 优惠券查询入参（管理端列表 / 用户端券包复用）
 */

@Data
public class CouponQueryDTO {

    /**页码，从1开始，默认1*/
    private Integer page = 1;

    /**每页条数，默认10*/
    private Integer size = 10;

    /**活动名称模糊搜索（管理端）*/
    private String activityName;

    /**券类型筛选（枚举）*/
    private CouponTypeEnum couponType;

    /**模板状态筛选：0未开始 1发放中 2已结束 3作废*/
    private Integer status;

    /**用户ID（查"我的券包"时必填）*/
    private Long userId;

    /**用户券使用状态筛选：0未开始 1未用 2已用 3已过期 4已退回*/
    private Integer useStatus;

    /**是否仅查可用券（true=只查未用且在有效期内的）*/
    private Boolean availableOnly;
}
