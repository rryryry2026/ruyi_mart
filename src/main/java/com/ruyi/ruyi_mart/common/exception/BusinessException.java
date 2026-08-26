package com.ruyi.ruyi_mart.common.exception;

import com.ruyi.ruyi_mart.common.enums.ResultCode;
import lombok.Getter;

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
