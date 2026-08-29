package com.ruyi.ruyi_mart.module.order.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.order.service.OrderService;
import com.ruyi.ruyi_mart.module.order.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result<OrderVO> create(){
        return Result.success(orderService.createOrder(currentUserId()));
    }

    @GetMapping("/list")
    public Result<List<OrderVO>> list(){
        return Result.success(orderService.listOrders(currentUserId()));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id){
        return Result.success(orderService.getOrderDetail(currentUserId(),id));
    }

    private Long currentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
