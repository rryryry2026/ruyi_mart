package com.ruyi.ruyi_mart.module.product.dto;

import lombok.Data;

@Data
public class ProductQueryDTO {
    /** 页码，默认 1（可不传） */
    private Integer pageNum;

    /** 每页条数，默认 10（可不传） */
    private Integer pageSize;

    /** 商品名称模糊搜索关键字（可不传） */
    private String keyword;

    /** 分类 ID 筛选（可不传） */
    private Long categoryId;

    /** 上架状态筛选：1=上架，0=下架（可不传） */
    private Integer status;
}
