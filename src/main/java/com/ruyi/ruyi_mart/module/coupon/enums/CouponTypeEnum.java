package com.ruyi.ruyi_mart.module.coupon.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CouponTypeEnum {

    /**1 - 满减券（满 min_spend 减 face_value）*/
    FULL_REDUCTION(1, "fullReduction", "满减券"),

    /**2 - 折扣券（打 discount_rate 折，最高减 max_discount，满 min_spend 可用）*/
    DISCOUNT(2, "discount", "折扣券"),

    /**3 - 无门槛券（直接减 face_value，无 min_spend）*/
    NO_THRESHOLD(3, "noThreshold", "无门槛券"),

    /**4 - 单品券（仅对 coupon_scope_detail 中指定商品生效，减 face_value）*/
    ITEM(4, "item", "单品券");

    /**数据库存储的数字。MyBatis-Plus 靠 @EnumValue 做 枚举 <-> DB 映射*/
    @EnumValue
    private final Integer code;

    /**JSON 序列化输出值（前端拿到的英文标识）*/
    @JsonValue
    private final String key;

    /**中文描述，用于日志 / 前端展示*/
    private final String desc;

    CouponTypeEnum(Integer code, String key, String desc) {
        this.code = code;
        this.key = key;
        this.desc = desc;
    }

    /**按 DB 数字反查枚举：业务代码手动把 Integer 转枚举时用*/
    public static CouponTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 按英文 key 反序列化：前端/接口传 "discount" 之类字符串时能转回枚举
     * 与 @JsonValue(key) 配对
     */
    @JsonCreator
    public static CouponTypeEnum fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (CouponTypeEnum type : values()) {
            if (type.getKey().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
