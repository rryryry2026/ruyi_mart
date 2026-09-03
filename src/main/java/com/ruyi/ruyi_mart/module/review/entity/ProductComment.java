package com.ruyi.ruyi_mart.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_comment")
public class ProductComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;
    private Long productSpecId;
    private String productSpecText;
    private String orderNo;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private Long parentId = 0L;
    private Long replyUserId;
    private String replyUserNickname;
    private int isBuyer = 0;
    private int isAppendComment = 0;
    private int isAnonymous = 0;
    private int isGoodReview = 0;
    private Byte rating = 0;
    private String content;
    private String imageUrls;
    private Integer likeCount = 0;

    @TableField(exist = false)
    private boolean like = false;

    private Byte status = 1;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}

