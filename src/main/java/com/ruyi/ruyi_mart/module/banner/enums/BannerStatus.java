package com.ruyi.ruyi_mart.module.banner.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.checkerframework.common.value.qual.EnumVal;

@Getter
public enum BannerStatus {

    ACTIVE("active", 1, "启用"),
    INACTIVE("inactive", 0, "禁用");

    @JsonValue
    private final String value;
    @EnumValue
    private final Integer number;
    private final String desc;

    BannerStatus(String value,Integer number,String desc){
        this.value = value;
        this.number = number;
        this.desc = desc;
    }

    public static BannerStatus getByValue(String value){
        for(BannerStatus status : values()){
            if(status.value.equals(value)){
                return status;
            }
        }
        throw new IllegalArgumentException("无效的BannerStatus.value:" + value);
    }

    @JsonCreator
    public static BannerStatus fromValue(String value){
        return getByValue(value);
    }
}
