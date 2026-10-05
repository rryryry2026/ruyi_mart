package com.ruyi.ruyi_mart.common.result;

import com.ruyi.ruyi_mart.common.enums.ResultCode;
import lombok.Data;

import java.io.Serializable;


/**统一响应格式 用静态工厂方法创建。*/
@Data
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    private Result(){}

    //构造器私有
    private Result(int code,String message,T data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    //静态工厂方法
    public static <T> Result<T> success(){
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data){
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> error(int code,String message){
        return new Result<>(code,message,null);
    }

    public static <T> Result<T> error(ResultCode resultCode){
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }


}
