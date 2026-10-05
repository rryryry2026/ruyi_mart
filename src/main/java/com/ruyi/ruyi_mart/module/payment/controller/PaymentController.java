package com.ruyi.ruyi_mart.module.payment.controller;


import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.common.util.SecurityUtils;
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

    /**从登录态取当前用户ID（未认证时 SecurityUtils 会直接抛 401）*/
    private Long currentUserId(){
        return SecurityUtils.currentUserId();
    }

    /**Mock支付模拟回调：点击支付页链接即视为付款成功*/
    @GetMapping("/mock/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> mockConfirm(@RequestParam Long orderId) {
        orderService.completePayment(orderId);
        return Result.success();
    }

    /**
     * 个人端模拟支付完成。
     *
     * 真实环境下"订单转为已支付"这一步由支付平台的异步通知触发，本项目没有对接真实渠道，
     * 所以给消费者端一个显式入口把这一步走完，否则下单后订单会一直停在待支付。
     *
     * 与上面的 /mock/confirm 的关键区别：那个是模拟"支付平台回调"，不需要用户身份、
     * 只按订单号操作，因此限管理员；这个需要登录态，且只允许确认自己的订单。
     */
    @PostMapping("/mock/pay/{orderId}")
    public Result<Void> mockPay(@PathVariable Long orderId) {
        orderService.confirmMockPayment(currentUserId(), orderId);
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
