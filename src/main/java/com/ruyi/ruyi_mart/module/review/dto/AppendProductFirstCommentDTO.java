package com.ruyi.ruyi_mart.module.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表追评的入参。
 */
@Data
public class AppendProductFirstCommentDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotBlank(message = "订单单号不能为空")
    private String orderNo;

    @NotBlank(message = "追评内容不能为空")
    @Size(max = 1000, message = "追评内容不能超过1000字")
    private String content;

    /**
     * 追评图片URL集合（JSON数组格式文本），非必填。库列是 varchar(2000)，超长会被数据库拒绝
     */
    @Size(max = 2000, message = "图片信息过长")
    private String imageUrls;
}
