package com.ruyi.ruyi_mart.module.payment.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentResult {

    /**支付方式：MOCK/ALIPAY/WECHAT*/
    private String payType;

    /**支付跳转链接，模拟环境下点这个链接即视为支付平台回调成功*/
    private String payUrl;

    /**订单ID*/
    private Long orderId;

    /**应付金额*/
    private BigDecimal amount;

    /**模拟二维码内容，前端可渲染成二维码图片*/
    private String qrContent;

    public PaymentResult() {
    }

    public PaymentResult(String payType, String payUrl, Long orderId, BigDecimal amount) {
        this.payType = payType;
        this.payUrl = payUrl;
        this.orderId = orderId;
        this.amount = amount;
    }
}
