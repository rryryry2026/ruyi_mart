package com.ruyi.ruyi_mart.module.payment.strategy;

import java.math.BigDecimal;

public interface PaymentStrategy {

    String type();

    boolean pay(Long orderId, Long userId, BigDecimal amount);
}
