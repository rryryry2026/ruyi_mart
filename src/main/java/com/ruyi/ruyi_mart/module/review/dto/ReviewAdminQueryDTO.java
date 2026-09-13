package com.ruyi.ruyi_mart.module.review.dto;

import lombok.Data;

/**
 * 管理端评价分页查询入参
 */
@Data
public class ReviewAdminQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**按商品ID精确筛选*/
    private Long productId;

    /**评论内容模糊搜索*/
    private String keyword;

    /**状态筛选：1=显示 0=隐藏；不传=全部*/
    private Integer status;

    /**评价类型筛选：1=好评 0=差评；不传=全部*/
    private Integer reviewType;
}
