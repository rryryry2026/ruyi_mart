package com.ruyi.ruyi_mart.module.payment.Payment;

import com.ruyi.ruyi_mart.module.payment.strategy.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class AlipayPaymentStrategy implements PaymentStrategy{

    public static final String TYPE = "ALIPAY";

    @Override
    public String type(){
        return TYPE;
    }

    @Override
    public boolean pay(Long orderId, Long userId, BigDecimal amount){
        log.info("[支付宝支付] 订单={}, 用户={}, 金额={} 发起支付（脚手架）", orderId, userId, amount);

        // TODO 真实接入：调 alipay SDK 统一收单下单，拿到支付表单/跳转 URL 返前端；
        //       异步回调 /pay/alipay/notify 验签通过后把订单置 PAID。
        // 当前为脚手架，先模拟成功；接真实 SDK 后改为"调用成功且拿到跳转 URL 才返回 true"
        return true;
    }
}
