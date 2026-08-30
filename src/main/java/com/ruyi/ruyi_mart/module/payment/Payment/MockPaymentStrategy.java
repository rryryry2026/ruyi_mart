package com.ruyi.ruyi_mart.module.payment.Payment;

import com.ruyi.ruyi_mart.module.payment.strategy.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class MockPaymentStrategy implements PaymentStrategy{

    public static final String TYPE="MOCK";

    @Override
    public String type(){
        return TYPE;
    }

    @Override
    public boolean pay(Long orderId, Long userId, BigDecimal amount){
        log.info("[Mock支付] 订单={}, 用户={}, 金额={} 模拟支付成功", orderId, userId, amount);
        return true;
    }

}
