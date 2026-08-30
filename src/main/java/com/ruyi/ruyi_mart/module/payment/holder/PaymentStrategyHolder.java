package com.ruyi.ruyi_mart.module.payment.holder;

import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.module.payment.strategy.PaymentStrategy;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyHolder {

    private final Map<String,PaymentStrategy> strategyMap;

    public PaymentStrategyHolder(List<PaymentStrategy> strategies){
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(PaymentStrategy::type,s -> s));
    }

    public PaymentStrategy get(String type){
        PaymentStrategy strategy = strategyMap.get(type);
        if(strategy == null){
            throw new BusinessException(ResultCode.FAIL,"不支持的支付方式" + type);
        }
        return strategy;
    }
}
