package com.ruyi.ruyi_mart.module.payment.strategy;

import com.ruyi.ruyi_mart.module.payment.vo.PaymentResult;

import java.math.BigDecimal;

/**支付策略接口*/
public interface PaymentStrategy {

    /**支付类型标识：MOCK/ALIPAY/WECHAT*/
    String type();

    /**发起支付，返回支付载体*/
    PaymentResult pay(Long orderId, Long userId, BigDecimal amount);
}
