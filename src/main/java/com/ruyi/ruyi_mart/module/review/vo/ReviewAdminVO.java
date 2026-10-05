package com.ruyi.ruyi_mart.module.review.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端评价视图（一级评论视角）
 * 注意：userNickname 是冗余存储的展示昵称，匿名评论在发表那一刻就存成了"匿名用户"，
 * 所以管理端看到的同样不是真实昵称；需要真人身份时按 userId 反查 user 表。
 */
@Data
public class ReviewAdminVO {

    private Long id;

    private Long productId;

    /**商品名称（关联 product 表）*/
    private String productName;

    private Long userId;

    private String userNickname;

    private String userAvatar;

    /**是否匿名(0/1)*/
    private Integer isAnonymous;

    /**是否买家(0/1)*/
    private Integer isBuyer;

    /**是否好评(0/1)*/
    private Integer isGoodReview;

    /**是否已追评(0/1)*/
    private Integer isAppendComment;

    /**评分 1~5 星*/
    private Integer rating;

    private String content;

    /**图片 JSON 数组文本*/
    private String imageUrls;

    private Integer likeCount;

    /**该评论下的二级回复条数*/
    private Long replyCount;

    /**1=显示 0=隐藏*/
    private Integer status;

    private LocalDateTime createTime;
}
