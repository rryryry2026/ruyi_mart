package com.ruyi.ruyi_mart.module.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FirstProductCommentDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 商品规格ID：ruyi_mart 商品无独立规格表，非必填，前端有则传
     */
    private Long productSpecId;

    /**
     * 商品规格文本（冗余存储），非必填
     */
    private String productSpecText;

    @NotBlank(message = "订单单号不能为空")
    private String orderNo;

    @NotBlank(message = "用户昵称不能为空")
    private String userNickname;

    @NotBlank(message = "用户头像不能为空")
    private String userAvatar;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000字")
    private String content;

    /**
     * 评论图片URL集合（JSON数组格式文本），非必填
     */
    private String imageUrls;

    @NotNull(message = "评分不能为空")
    private Integer rating;

    /**
     * 是否匿名评论（0=否，1=是），默认否
     */
    private int isAnonymous = 0;
}
