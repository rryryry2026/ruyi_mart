package com.ruyi.ruyi_mart.module.payment.controller;


import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**支付回调控制器，接收支付平台异步通知，触发订单完成支付*/
@RestController
@RequestMapping("/payment")
public class PaymentController {

    /**订单服务*/
    @Autowired
    private OrderService orderService;

    /**Mock支付模拟回调：点击支付页链接即视为付款成功*/
    @GetMapping("/mock/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> mockConfirm(@RequestParam Long orderId) {
        orderService.completePayment(orderId);
        return Result.success();
    }

    /**支付宝模拟异步回调通知*/
    @PostMapping("/alipay/notify")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> alipayNotify(@RequestParam Long orderId) {
        orderService.completePayment(orderId);
        return Result.success();
    }

    /**微信模拟异步回调通知*/
    @PostMapping("/wechat/notify")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> wechatNotify(@RequestParam Long orderId) {
        orderService.completePayment(orderId);
        return Result.success();
    }
}
