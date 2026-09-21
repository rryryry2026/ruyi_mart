package com.ruyi.ruyi_mart.common.exception;

import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务处理异常。
     * HTTP 状态码跟着业务错误码走：以前这里固定返回 200，失败和成功在状态码上长得一模一样，
     * 网关、监控、前端都只能去解析响应体才知道出没出错。
     * 响应体结构保持不变（code + message），前端原有的判断逻辑不用改。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e){
        return ResponseEntity.status(httpStatusOf(e.getCode()))
                .body(Result.error(e.getCode(), e.getMessage()));
    }

    /**业务错误码 → HTTP 状态码*/
    private HttpStatus httpStatusOf(int code){
        if(code == ResultCode.UNAUTHORIZED.getCode()){
            return HttpStatus.UNAUTHORIZED;
        }
        if(code == ResultCode.FORBIDDEN.getCode()){
            return HttpStatus.FORBIDDEN;
        }
        if(code == ResultCode.NOT_FIND.getCode()){
            return HttpStatus.NOT_FOUND;
        }
        if(code == ResultCode.ERROR.getCode()){
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }

    /**参数校验异常。*/
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e){
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.error(ResultCode.FAIL.getCode(), msg);
    }

    /**
     * 请求参数类异常：缺必填参数、参数类型不对、JSON 体解析失败、路径参数校验不通过。
     * 这几类都属于"调用方传错了"，以前没人接就落到兜底 500 —— 明明是 400 的语义，
     * 却报成"服务器内部错误"，排查时会被带偏。
     */
    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            BindException.class,
            ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBadRequest(Exception e){
        log.warn("请求参数不合法: {}", e.getMessage());
        return Result.error(ResultCode.FAIL.getCode(), "请求参数不合法");
    }

    /**兜底异常 对外模糊，对内详细 前端拿到结构一致的json。*/
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e){
        log.error("Unhandled exception", e);
        return Result.error(ResultCode.ERROR);
    }

    /**权限不足异常。*/
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedExpection(AccessDeniedException e){
        return Result.error(ResultCode.FORBIDDEN);
    }

}
