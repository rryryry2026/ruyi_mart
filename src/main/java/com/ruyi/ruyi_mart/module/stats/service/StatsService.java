package com.ruyi.ruyi_mart.module.stats.service;

import com.ruyi.ruyi_mart.module.stats.vo.StatsSummaryVO;

public interface StatsService {

    /**工作台概览：今日订单/销售额 + 待发货/待审退款/低库存计数 + 订单状态分布*/
    StatsSummaryVO summary();
}
