package com.ruyi.ruyi_mart.module.stats.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.stats.service.StatsService;
import com.ruyi.ruyi_mart.module.stats.vo.StatsSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端工作台统计接口
 */
@RestController
@RequestMapping("/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    @Autowired
    private StatsService statsService;

    /**工作台概览指标 + 待办计数 + 订单状态分布*/
    @GetMapping("/summary")
    public Result<StatsSummaryVO> summary() {
        return Result.success(statsService.summary());
    }
}
