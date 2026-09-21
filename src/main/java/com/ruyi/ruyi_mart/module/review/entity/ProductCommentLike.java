package com.ruyi.ruyi_mart.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**商品评价点赞表 product_comment_like 的镜像。一个用户对一条评论一条记录。*/
@Data
@TableName("product_comment_like")
public class ProductCommentLike {

    @TableId(type = IdType.AUTO)
    private Long id;
    /**评论ID*/
    private Long commentId;
    /**点赞人ID*/
    private Long userId;
    /**1点赞 0取消（取消不删记录，改状态，便于再次点赞）*/
    private Integer status = 1;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
