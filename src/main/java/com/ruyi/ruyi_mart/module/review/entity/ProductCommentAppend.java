package com.ruyi.ruyi_mart.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_comment_append")
public class ProductCommentAppend {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private Long productId;
    private Long productSpecId;
    private String orderNo;
    private Long userId;
    private String content;
    private String imageUrls;
    private Byte status = 1;
    private LocalDateTime createTime;
}

