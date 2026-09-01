package com.ruyi.ruyi_mart.module.review.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductFirstCommentVO {

    /** 评论 id */
    private Long id;

    /** 商品 id */
    private Long productId;

    /** 商品规格 id（冗余存储，便于展示） */
    private Long productSpecId;

    /** 商品规格简介文本（如 "颜色:黑 容量:256G"） */
    private String productSpecText;

    /** 评论用户 id */
    private Long userId;

    /** 用户昵称（冗余存储，展示用，不 join 用户表） */
    private String userNickname;

    /** 用户头像（冗余存储） */
    private String userAvatar;

    /** 是否为买家(0/1) */
    private Integer isBuyer;

    /** 是否已追评(0/1) */
    private Integer isAppendComment;

    /** 是否匿名(0/1) */
    private Integer isAnonymous;

    /** 是否好评(0/1) */
    private Integer isGoodReview;

    /** 评分 1~5 星 */
    private Integer rating;

    /** 评论内容 */
    private String content;

    /** 评论图片（JSON 数组格式文本，例 ["url1","url2"]） */
    private String imageUrls;

    /** 点赞总数 */
    private Integer likeCount;

    /** 当前用户是否点赞（非表字段，由 Service 按点赞表填充） */
    private boolean like = false;

    /** 创建时间 */
    private LocalDateTime createTime;
}
