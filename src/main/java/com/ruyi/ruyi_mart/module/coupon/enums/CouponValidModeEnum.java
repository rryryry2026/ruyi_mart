package com.ruyi.ruyi_mart.module.coupon.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CouponValidModeEnum {

    /**1 - 固定时间（在 valid_start ~ valid_end 之间有效）*/
    FIXED_TIME(1, "fixedTime", "固定时间"),

    /**2 - 领券后 N 天有效（N = receive_valid_days）*/
    AFTER_RECEIVE(2, "afterReceive", "领券后N天");

    /**数据库存储的数字。MyBatis-Plus 靠 @EnumValue 做 枚举 <-> DB 映射*/
    @EnumValue
    private final Integer code;

    /**JSON 序列化输出值（前端拿到的英文标识）*/
    @JsonValue
    private final String key;

    /**中文描述，用于日志 / 前端展示*/
    private final String desc;

    CouponValidModeEnum(Integer code, String key, String desc) {
        this.code = code;
        this.key = key;
        this.desc = desc;
    }

    /**按 DB 数字反查枚举：业务代码手动把 Integer 转枚举时用*/
    public static CouponValidModeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponValidModeEnum mode : values()) {
            if (mode.getCode().equals(code)) {
                return mode;
            }
        }
        return null;
    }

    /**按英文 key 反序列化：前端/接口传 "fixedTime" 之类字符串时能转回枚举
     * 与 @JsonValue(key) 配对
     */
    @JsonCreator
    public static CouponValidModeEnum fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (CouponValidModeEnum mode : values()) {
            if (mode.getKey().equalsIgnoreCase(key)) {
                return mode;
            }
        }
        return null;
    }
}
