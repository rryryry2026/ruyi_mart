package com.ruyi.ruyi_mart.module.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.common.util.SecurityUtils;
import com.ruyi.ruyi_mart.module.log.annotation.OpLog;
import com.ruyi.ruyi_mart.module.order.dto.OrderCreateDTO;
import com.ruyi.ruyi_mart.module.order.service.OrderService;
import com.ruyi.ruyi_mart.module.order.vo.OrderVO;
import com.ruyi.ruyi_mart.module.payment.vo.PaymentResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto){
        return Result.success(orderService.createOrder(currentUserId(), dto));
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
    public Result<PaymentResult> pay(@PathVariable Long orderId, @RequestParam String payType){
        // payType 必传：默认值 MOCK 会让生产环境在调用方漏传时静默走模拟支付
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
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) Integer status){
        // 参数名与管理端统一为 pageSize：原来叫 pagesize（全小写），
        // 前端传错名字时 Spring 匹配不上、不报错、静默用默认值
        return  Result.success(orderService.listOrdersPage(currentUserId(),status,pageNum,pageSize));
    }

    @PostMapping("/ship/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @OpLog(module = "订单管理", action = "发货")
    public Result<OrderVO> ship(@PathVariable Long orderId){
        return Result.success(orderService.shipOrder(orderId));
    }

    @PostMapping("/confirm/{orderId}")
    public Result<OrderVO> confirm(@PathVariable Long orderId){
        return Result.success(orderService.confirmReceive(currentUserId(),orderId));
    }


    private Long currentUserId(){
        return SecurityUtils.currentUserId();
    }
}
