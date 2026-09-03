package com.ruyi.ruyi_mart.module.banner.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruyi.ruyi_mart.module.banner.enums.BannerStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("banner")
public class Banner {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 轮播图标题 */
    private String title;

    /** 图片URL */
    @TableField("image_url")
    private String imageUrl;

    /** 跳转链接 */
    @TableField("link_url")
    private String linkUrl;

    /** 链接类型(product/category/url) */
    @TableField("link_type")
    private String linkType;

    /** 排序*/
    private Integer sort = 0;

    /** 状态 */
    private BannerStatus status = BannerStatus.ACTIVE;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
