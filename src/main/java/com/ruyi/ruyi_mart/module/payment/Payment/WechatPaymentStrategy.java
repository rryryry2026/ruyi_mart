package com.ruyi.ruyi_mart.module.payment.Payment;

import com.ruyi.ruyi_mart.module.payment.strategy.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class WechatPaymentStrategy implements PaymentStrategy{

    public static final String TYPE = "WECHAT";

    @Override
    public String type(){
        return TYPE;
    }

    @Override
    public boolean pay(Long orderId, Long userId, BigDecimal amount){
        log.info("[微信支付] 订单={}, 用户={}, 金额={} 发起支付（V3 脚手架）", orderId, userId, amount);

        // TODO 真实接入步骤（wechatpay-java SDK）：
        // 1. 金额单位转换：元 → 分，new Amount("CNY", amount.multiply(new BigDecimal("100")).intValue())
        // 2. 构造请求：PrepayRequest（outTradeNo=订单号, description, notifyUrl, amount）
        // 3. 调 SDK：nativePayService.prepay(request) 或 jsapiService.prepay(request) 拿 PrepayResponse
        // 4. 把 response.codeUrl / prepayId 返回前端，由前端调起微信支付
        // 5. 微信异步回调 /pay/wechat/notify → 平台证书验签 → 订单置 PAID（不在这里同步处理）

        // 当前为脚手架，先模拟成功；接真实 SDK 后改为"调用成功且拿到 codeUrl 才返回 true"

        return true;
    }
}
