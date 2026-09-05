package com.ruyi.ruyi_mart.module.payment.Payment;

import com.ruyi.ruyi_mart.module.payment.strategy.PaymentStrategy;
import com.ruyi.ruyi_mart.module.payment.vo.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class MockPaymentStrategy implements PaymentStrategy{

    public static final String TYPE="MOCK";

    private static final String CONFIRM_URL = "/payment/mock/confirm";

    @Override
    public String type(){
        return TYPE;
    }

    @Override
    public PaymentResult pay(Long orderId, Long userId, BigDecimal amount) {
        log.info("[Mock支付] 发起支付 订单={}, 用户={}, 金额={}", orderId, userId, amount);
        String payUrl = CONFIRM_URL + "?orderId=" + orderId;
        String qrContent = "MOCK_PAY:orderId=" + orderId + "&amount=" + amount;
        PaymentResult result = new PaymentResult(TYPE, payUrl, orderId, amount);
        result.setQrContent(qrContent);
        return result;
    }
}
