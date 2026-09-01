package com.ruyi.ruyi_mart.module.review.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductAppendCommentVO {

    /** 追评 id */
    private Long id;

    /** 追评用户 id */
    private Long userId;

    /** 追评内容 */
    private String content;

    /** 追评图片（JSON 数组格式文本） */
    private String imageUrls;

    /** 追评创建时间 */
    private LocalDateTime createTime;
}
