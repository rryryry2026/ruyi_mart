package com.ruyi.ruyi_mart.module.refund.enums;

public enum RefundStatus {

    PENDING(0, "待审核"),
    REFUNDED(1, "已退款"),
    REJECTED(2, "已拒绝");

    private final int code;
    private final String desc;

    RefundStatus(int code, String desc){
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
