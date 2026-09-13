package com.ruyi.ruyi_mart.module.log.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 操作日志分页查询入参
 */
@Data
public class OperationLogQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    /** 操作人用户名模糊搜索 */
    private String username;

    /** 业务模块精确筛选 */
    private String module;

    /** 是否成功：1成功 0失败；不传=全部（排查问题时通常只看失败） */
    private Integer success;

    /** 操作日期起（含当天） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startTime;

    /** 操作日期止（含当天） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endTime;
}
