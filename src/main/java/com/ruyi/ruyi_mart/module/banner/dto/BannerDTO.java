package com.ruyi.ruyi_mart.module.banner.dto;

import com.ruyi.ruyi_mart.module.banner.enums.BannerStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BannerDTO {

    /** 轮播图标题（可选） */
    private String title;

    /** 图片URL（必填,对应表 image_url NOT NULL） */
    @NotBlank(message = "图片URL不能为空")
    private String imageUrl;

    /** 跳转链接（可选） */
    private String linkUrl;

    /** 链接类型(product/category/url)，可选 */
    private String linkType;

    /** 排序,数字越小越靠前；可选，默认0 */
    private Integer sort;

    /** 状态:启用/禁用；可选，默认启用(ACTIVE) */
    private BannerStatus status;
}
