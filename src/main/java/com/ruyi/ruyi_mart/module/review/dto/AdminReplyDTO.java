package com.ruyi.ruyi_mart.module.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端商家回复入参。
 * 刻意不复用用户端的 SecondProductCommentDTO：那个 DTO 的 userNickname 由前端传入，
 * 存在伪造身份的风险；管理端回复的昵称一律从数据库取当前管理员本人。
 */
@Data
public class AdminReplyDTO {

    /**被回复的一级评论ID*/
    @NotNull(message = "父评论ID不能为空")
    private Long parentId;

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复内容长度不能超过500个字符")
    private String content;
}
