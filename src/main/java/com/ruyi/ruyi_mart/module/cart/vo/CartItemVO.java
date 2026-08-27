package com.ruyi.ruyi_mart.module.cart.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVO {

    private Long productId;        // 商品id（= Hash 的 field）
    private String name;           // 商品名（加购时快照）
    private BigDecimal price;      // 加购时单价快照
    private String imageUrl;       // 商品图
    private Integer quantity;      // 数量
    private BigDecimal subtotal;   // 小计 = price * quantity（下单行算，方便前端展示）
}
