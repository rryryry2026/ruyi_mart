package com.ruyi.ruyi_mart.module.coupon.dto;

import com.ruyi.ruyi_mart.module.coupon.enums.CouponTypeEnum;
import lombok.Data;


/**查询优惠券的入参。*/
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
}
