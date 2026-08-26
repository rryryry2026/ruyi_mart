package com.ruyi.ruyi_mart.common.enums;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200,"success"),
    FAIL(400,"请求失败"),
    UNAUTHORIZED(401,"未登录或登录已过期"),
    FORBIDDEN(403,"无权限访问"),
    NOT_FIND(404,"资源不存在"),
    ERROR(500,"服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code,String message){
        this.code = code;
        this.message = message;
    }

}
