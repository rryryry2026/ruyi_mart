package com.ruyi.ruyi_mart.module.banner.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BannerSortDTO {

    /** 轮播图ID列表,按期望展示顺序从前到后排列 */
    @NotEmpty(message = "轮播图ID列表不能为空")
    private List<Long> ids;
}
