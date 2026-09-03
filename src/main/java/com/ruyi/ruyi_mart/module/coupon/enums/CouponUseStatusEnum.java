package com.ruyi.ruyi_mart.module.coupon.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CouponUseStatusEnum {

    /**0 - 未开始（领到了但还没到生效时间）*/
    UN_BEGIN(0, "unBegin", "未开始"),

    /**1 - 未使用（已生效，可核销）*/
    UNUSED(1, "unused", "未用"),

    /*** 2 - 已使用*/
    USED(2, "used", "已用"),

    /*** 3 - 已过期*/
    EXPIRED(3, "expired", "已过期"),

    /*** 4 - 已退回 / 已返还*/
    RETURNED(4, "returned", "已退回");

    /*** 数据库存储的数字。MyBatis-Plus 靠 @EnumValue 做 枚举 <-> DB 映射*/
    @EnumValue
    private final Integer code;

    /*** JSON 序列化输出值（前端拿到的英文标识）*/
    @JsonValue
    private final String key;

    /*** 中文描述，用于日志 / 前端展示*/
    private final String desc;

    CouponUseStatusEnum(Integer code, String key, String desc) {
        this.code = code;
        this.key = key;
        this.desc = desc;
    }

    /**
     * 按 DB 数字反查枚举：业务代码手动把 Integer 转枚举时用
     * （如 @Scheduled 扫到某行 use_status，想拿到枚举判断）
     */
    public static CouponUseStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponUseStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 按英文 key 反序列化：前端/接口传 "unused" 之类字符串时能转回枚举
     * 与 @JsonValue(key) 配对
     */
    @JsonCreator
    public static CouponUseStatusEnum fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (CouponUseStatusEnum status : values()) {
            if (status.getKey().equalsIgnoreCase(key)) {
                return status;
            }
        }
        return null;
    }


}
