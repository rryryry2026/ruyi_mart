package com.ruyi.ruyi_mart.module.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    @PostMapping("/pay/{orderId}")
    public Result<OrderVO> pay(@PathVariable Long orderId,@RequestParam(required = false,defaultValue = "MOCK") String payType){
        return Result.success(orderService.payOrder(currentUserId(),orderId,payType));
    }

    @PostMapping("/cancel/{orderId}")
    public Result<OrderVO> cancel(@PathVariable Long orderId){
        return  Result.success(orderService.cancelOrder(currentUserId(),orderId));
    }

    @GetMapping("/list/status/{status}")
    public Result<List<OrderVO>> listByStatus(@PathVariable Integer status){
        return Result.success(orderService.listOrdersByStatus(currentUserId(),status));
    }

    @GetMapping("/list/page")
    public Result<Page<OrderVO>> listPage(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pagesize,
                                          @RequestParam(required = false) Integer status){
        return  Result.success(orderService.listOrdersPage(currentUserId(),status,pageNum,pagesize));

    }


    private Long currentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
