package com.ruyi.ruyi_mart.module.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SecondProductCommentDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "父评论ID不能为空")
    private Long parentId;

    @Size(max = 20, message = "用户昵称长度不能超过20个字符")
    private String userNickname;

    private String userAvatar;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容长度不能超过500个字符")
    private String content;

    /**
     * 回复图片URL集合（JSON数组格式文本），非必填
     */
    private String imageUrls;

    /**
     * 被回复用户ID（二级专用，前端展示「回复@XXX」）
     */
    private Long replyUserId;

    /**
     * 被回复用户昵称（冗余存储，展示用）
     */
    private String replyUserNickname;

    /**
     * 是否匿名评论（0=否，1=是），默认否
     */
    private int isAnonymous = 0;
}
