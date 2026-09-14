package com.ruyi.ruyi_mart.module.hotkeyword.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HotKeywordDTO {

    @NotBlank(message = "关键词不能为空")
    @Size(max = 30, message = "关键词不能超过30字")
    private String keyword;

    /** 排序，数字越小越靠前；可选，默认 0 */
    private Integer sort;

    /** 状态：1 启用，0 禁用；可选，默认 1 */
    private Integer status;
}
