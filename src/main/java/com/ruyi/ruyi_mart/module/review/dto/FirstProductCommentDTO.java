package com.ruyi.ruyi_mart.module.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表一级评论（首评）的入参。
 * 昵称、头像、"是否买家"都不在前端传 —— 由服务端按 userId 查库、按订单校验后自己填，
 * 否则任何人都能自称任意昵称、并给自己标上"已购买"。
 */
@Data
public class FirstProductCommentDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**商品规格ID：本项目商品无独立规格表，非必填*/
    private Long productSpecId;

    /**商品规格文本（冗余存储），非必填*/
    private String productSpecText;

    /**下单时的订单号：服务端用它校验"这单是你的、里面有这个商品、且已完成"*/
    @NotBlank(message = "订单单号不能为空")
    private String orderNo;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000字")
    private String content;

    /**评论图片URL集合（JSON数组格式文本），非必填。库列是 varchar(2000)，超长会被数据库拒绝*/
    @Size(max = 2000, message = "图片信息过长")
    private String imageUrls;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1星")
    @Max(value = 5, message = "评分最高5星")
    private Integer rating;

    /**是否匿名评论（0=否，1=是），默认否*/
    private Integer isAnonymous = 0;
}
