package com.ruyi.ruyi_mart.module.stock.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端库存列表视图：商品信息 + 库存明细（无库存记录的商品 stock 字段为 null）
 */
@Data
public class ProductStockVO {

    private Long productId;

    private String productName;

    private String imageUrl;

    private Long categoryId;

    private String categoryName;

    /**库存总量；null=未初始化库存*/
    private Integer total;

    /**可用库存；null=未初始化库存*/
    private Integer available;

    /**未支付订单预扣数量；null=未初始化库存*/
    private Integer locked;

    private LocalDateTime updateTime;
}
