package com.ruyi.ruyi_mart.module.payment.Payment;

import com.ruyi.ruyi_mart.module.payment.strategy.PaymentStrategy;
import com.ruyi.ruyi_mart.module.payment.vo.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class WechatPaymentStrategy implements PaymentStrategy{

    public static final String TYPE = "WECHAT";

    private static final String NOTIFY_URL = "/payment/wechat/notify";

    @Override
    public String type(){
        return TYPE;
    }

    @Override
    public PaymentResult pay(Long orderId, Long userId, BigDecimal amount) {
        log.info("[微信支付] 发起支付 订单={}, 用户={}, 金额={}", orderId, userId, amount);
        String payUrl = NOTIFY_URL + "?orderId=" + orderId;
        String qrContent = "WECHAT_PAY:orderId=" + orderId + "&amount=" + amount;
        PaymentResult result = new PaymentResult(TYPE, payUrl, orderId, amount);
        result.setQrContent(qrContent);
        return result;
    }
}
