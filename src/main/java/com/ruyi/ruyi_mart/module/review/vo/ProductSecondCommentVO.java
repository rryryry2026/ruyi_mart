package com.ruyi.ruyi_mart.module.review.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductSecondCommentVO {

    /** 回复 id */
    private Long id;

    /** 商品 id */
    private Long productId;

    /** 用户昵称（冗余存储） */
    private String userNickname;

    /** 用户头像（冗余存储） */
    private String userAvatar;

    /** 是否为买家(0/1) */
    private Integer isBuyer;

    /** 是否商家回复(0/1)：作者是管理员即视为商家，前端据此加"商家"徽标 */
    private Integer isSeller;

    /** 是否匿名(0/1) */
    private Integer isAnonymous;

    /** 回复内容 */
    private String content;

    /** 回复图片（JSON 数组格式文本） */
    private String imageUrls;

    /** 点赞总数 */
    private Integer likeCount;

    /** 当前用户是否点赞（非表字段，由 Service 填充） */
    private boolean like = false;

    /** 被回复用户昵称（冗余存储）。不回 userId：前端只需要昵称，少暴露一个可反查的字段 */
    private String replyUserNickname;

    /** 创建时间 */
    private LocalDateTime createTime;
}
