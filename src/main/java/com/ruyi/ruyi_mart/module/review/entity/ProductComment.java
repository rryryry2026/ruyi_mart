package com.ruyi.ruyi_mart.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**商品评价表 product_comment 的镜像。一级评论（parent_id=0）与二级回复同表，靠 parent_id 区分。*/
@Data
@TableName("product_comment")
public class ProductComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**商品ID*/
    private Long productId;
    /**商品规格ID（本项目商品无独立规格表，通常为空）*/
    private Long productSpecId;
    /**商品规格文本（冗余存储）*/
    private String productSpecText;
    /**下单时的订单号：首评用它证明"确实买过"*/
    private String orderNo;
    /**评论人ID*/
    private Long userId;
    /**评论人昵称（取数据库里的真实昵称；匿名时存"匿名用户"）*/
    private String userNickname;
    /**评论人头像。user 表没有头像字段，所以这里恒为空，由前端用昵称首字兜底*/
    private String userAvatar;
    /**父评论ID：0=一级评论，>0=二级回复*/
    private Long parentId = 0L;
    /**被回复人ID（二级回复专用）*/
    private Long replyUserId;
    /**被回复人昵称，取父评论里存的昵称（父评论匿名时它同样是"匿名用户"）*/
    private String replyUserNickname;
    /**是否买家（0否1是）：首评通过订单校验后才置 1*/
    private Integer isBuyer = 0;
    /**是否已追评（0否1是）*/
    private Integer isAppendComment = 0;
    /**是否匿名（0否1是）*/
    private Integer isAnonymous = 0;
    /**是否好评（评分>=4）*/
    private Integer isGoodReview = 0;
    /**评分1~5星，二级回复固定0*/
    private Integer rating = 0;
    /**评论内容*/
    private String content;
    /**评论图片，JSON数组文本*/
    private String imageUrls;
    /**点赞总数*/
    private Integer likeCount = 0;

    /**当前登录用户是否点过赞。不是数据库字段，只用于返回给前端*/
    @TableField(exist = false)
    private boolean like = false;

    /**审核状态：1通过 0待审 2驳回（本项目无审核端，默认通过，管理端可改）*/
    private Integer status = 1;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
