package com.ruyi.ruyi_mart.module.order.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 管理端订单分页查询入参
 */
@Data
public class OrderAdminQueryDTO {

    /**页码，从1开始*/
    private Integer pageNum = 1;

    /**每页条数*/
    private Integer pageSize = 10;

    /**订单号（精确匹配）*/
    private String orderNo;

    /**买家关键字（用户名/昵称模糊）*/
    private String keyword;

    /**订单状态*/
    private Integer status;

    /**下单日期起（含当天）*/
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startTime;

    /**下单日期止（含当天）*/
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endTime;
}
