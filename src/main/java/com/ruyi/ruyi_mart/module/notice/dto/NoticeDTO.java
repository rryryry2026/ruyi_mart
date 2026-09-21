package com.ruyi.ruyi_mart.module.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**公告dto对象。*/
@Data
public class NoticeDTO {

    /** 公告标题（滚动栏显示的就是它） */
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "公告标题不能超过100字")
    private String title;

    /** 公告内容，可选 */
    @Size(max = 2000, message = "公告内容不能超过2000字")
    private String content;

    /** 排序，数字越小越靠前；可选，默认 0 */
    private Integer sort;

    /** 状态：1 启用，0 禁用；可选，默认 1 */
    private Integer status;
}
