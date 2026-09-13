package com.ruyi.ruyi_mart.module.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {

    /** 业务模块名，为空则按请求路径自动推断 */
    String module() default "";

    /** 操作类型，为空则按 HTTP 方法自动推断（POST→新增、PUT→修改、DELETE→删除） */
    String action() default "";
}
