package com.ruyi.ruyi_mart.module.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表二级回复的入参。
 * 昵称、头像、被回复人同样不由前端传：昵称按 userId 查库，
 * 被回复人直接取父评论里已经存好的那份（父评论匿名时它就是"匿名用户"）。
 */
@Data
public class SecondProductCommentDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "父评论ID不能为空")
    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容长度不能超过500个字符")
    private String content;

    /**回复图片URL集合（JSON数组格式文本），非必填。库列是 varchar(2000)，超长会被数据库拒绝*/
    @Size(max = 2000, message = "图片信息过长")
    private String imageUrls;

    /**是否匿名评论（0=否，1=是），默认否*/
    private Integer isAnonymous = 0;
}
