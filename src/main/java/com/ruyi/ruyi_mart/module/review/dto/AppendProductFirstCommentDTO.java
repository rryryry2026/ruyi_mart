package com.ruyi.ruyi_mart.module.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
     * 追评图片URL集合（JSON数组格式文本），非必填
     */
    private String imageUrls;
}
