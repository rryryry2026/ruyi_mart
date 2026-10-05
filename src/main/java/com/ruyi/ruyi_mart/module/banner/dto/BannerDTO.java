package com.ruyi.ruyi_mart.module.banner.dto;

import com.ruyi.ruyi_mart.module.banner.enums.BannerStatus;
import lombok.Data;

/**接收前端传来的轮播图参数。*/
@Data
public class BannerDTO {

    /** 轮播图标题（可选） */
    private String title;

    /** 图片URL（新建时必填，改用服务端校验——DTO 上加 @NotBlank 会把"局部更新"逼成整体更新） */
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
