package com.ruyi.ruyi_mart.module.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.order.dto.OrderAdminQueryDTO;
import com.ruyi.ruyi_mart.module.order.service.OrderService;
import com.ruyi.ruyi_mart.module.order.vo.OrderAdminVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端订单接口：管理员视角查全量订单（不限定买家）
 */
@RestController
@RequestMapping("/admin/order")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    /**全量订单分页（订单号/买家/状态/时间范围筛选）*/
    @GetMapping("/page")
    public Result<Page<OrderAdminVO>> page(OrderAdminQueryDTO dto) {
        return Result.success(orderService.adminListOrders(dto));
    }

    /**订单详情（管理员视角，不校验订单归属）*/
    @GetMapping("/{id}")
    public Result<OrderAdminVO> detail(@PathVariable Long id) {
        return Result.success(orderService.adminGetOrderDetail(id));
    }
}
