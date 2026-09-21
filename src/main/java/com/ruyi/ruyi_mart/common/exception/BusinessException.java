package com.ruyi.ruyi_mart.common.exception;

import com.ruyi.ruyi_mart.common.enums.ResultCode;
import lombok.Getter;

/**
 * 业务异常 继承非受检异常，出现异常往上抛。
 * messgae字段和方法父类有，继承下来。
 */
@Getter
public class BusinessException extends RuntimeException{

    private final int code;

    public BusinessException(ResultCode resultCode){
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode,String message){
        super(message);
        this.code = resultCode.getCode();
    }
}
