package com.ruyi.ruyi_mart.module.banner.dto;

import com.ruyi.ruyi_mart.module.banner.enums.BannerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**接收前端传的改轮播图状态参数。*/
@Data
public class BannerStatusDTO {

    /** 目标状态:启用(active)/禁用(inactive)，必传 */
    @NotNull(message = "状态不能为空")
    private BannerStatus status;
}
