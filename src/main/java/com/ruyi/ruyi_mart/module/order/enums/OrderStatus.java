package com.ruyi.ruyi_mart.module.order.enums;

public enum OrderStatus {

    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    STOCK_FAILED(2, "库存不足"),
    CLOSED(3, "已关闭");

    private final int code;
    private final String desc;

    OrderStatus(int code,String desc){
        this.code = code;
        this.desc = desc;
    }

    public int getCode(){
        return code;
    }

    public String getDesc(){
        return desc;
    }
}
