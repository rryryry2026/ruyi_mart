package com.ruyi.ruyi_mart.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**商品评价追评表 product_comment_append 的镜像。一条一级评论最多一条追评。*/
@Data
@TableName("product_comment_append")
public class ProductCommentAppend {

    @TableId(type = IdType.AUTO)
    private Long id;
    /**被追评的一级评论ID*/
    private Long commentId;
    private Long productId;
    private Long productSpecId;
    private String orderNo;
    /**追评人ID（与首评同一个人）*/
    private Long userId;
    private String content;
    /**追评图片，JSON数组文本*/
    private String imageUrls;
    private Integer status = 1;
    private LocalDateTime createTime;
}
