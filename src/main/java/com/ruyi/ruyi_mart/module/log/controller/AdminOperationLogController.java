package com.ruyi.ruyi_mart.module.log.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.log.dto.OperationLogQueryDTO;
import com.ruyi.ruyi_mart.module.log.entity.OperationLog;
import com.ruyi.ruyi_mart.module.log.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;

/**
 * 操作日志查询接口。
 * 只提供查询——审计日志如果可被修改或删除就失去意义了。
 */
@RestController
@RequestMapping("/admin/log")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperationLogController {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /** 操作日志分页（操作人/模块/成败/时间范围筛选） */
    @GetMapping("/page")
    public Result<Page<OperationLog>> page(OperationLogQueryDTO dto) {
        LambdaQueryWrapper<OperationLog> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getUsername())) {
            qw.like(OperationLog::getUsername, dto.getUsername());
        }
        if (StringUtils.hasText(dto.getModule())) {
            qw.eq(OperationLog::getModule, dto.getModule());
        }
        if (dto.getSuccess() != null) {
            qw.eq(OperationLog::getSuccess, dto.getSuccess());
        }
        if (dto.getStartTime() != null) {
            qw.ge(OperationLog::getCreateTime, dto.getStartTime().atStartOfDay());
        }
        if (dto.getEndTime() != null) {
            // 结束日期取当天最后一刻，否则查不到当天操作（日期范围的经典边界问题）
            qw.le(OperationLog::getCreateTime, dto.getEndTime().atTime(LocalTime.MAX));
        }
        qw.orderByDesc(OperationLog::getCreateTime).orderByDesc(OperationLog::getId);
        return Result.success(operationLogMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), qw));
    }
}
