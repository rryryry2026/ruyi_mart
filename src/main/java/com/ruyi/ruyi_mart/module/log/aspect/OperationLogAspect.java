package com.ruyi.ruyi_mart.module.log.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.ruyi_mart.module.log.annotation.OpLog;
import com.ruyi.ruyi_mart.module.log.entity.OperationLog;
import com.ruyi.ruyi_mart.module.log.mapper.OperationLogMapper;
import com.ruyi.ruyi_mart.module.user.entity.User;
import com.ruyi.ruyi_mart.module.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面：自动记录管理端的写操作。
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /** 参数中命中这些关键字的值不落库，避免密码/密钥进审计表 */
    private static final String[] SENSITIVE_KEYS = {"password", "oldPassword", "newPassword", "token", "secret", "accessKey"};
    /** 参数序列化后的最大长度，防止长文本把日志表撑爆 */
    private static final int PARAMS_MAX_LENGTH = 1800;
    /** 异常信息最大长度 */
    private static final int ERROR_MAX_LENGTH = 450;

    /** 请求路径首段 → 中文模块名，让日志不写注解也能读懂 */
    private static final Map<String, String> MODULE_NAMES = new HashMap<>();

    static {
        MODULE_NAMES.put("order", "订单管理");
        MODULE_NAMES.put("refund", "退款审核");
        MODULE_NAMES.put("product", "商品管理");
        MODULE_NAMES.put("category", "分类管理");
        MODULE_NAMES.put("stock", "库存管理");
        MODULE_NAMES.put("banner", "轮播图管理");
        MODULE_NAMES.put("coupon", "优惠券管理");
        MODULE_NAMES.put("review", "评价管理");
        MODULE_NAMES.put("user", "用户管理");
        MODULE_NAMES.put("log", "操作日志");
        MODULE_NAMES.put("payment", "支付");
        MODULE_NAMES.put("api", "工具");
    }

    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private UserMapper userMapper;
    /** 复用 Spring 容器里已按 application.yaml 配置好的 ObjectMapper，避免每次 new */
    @Autowired
    private ObjectMapper objectMapper;

    /** 切所有 controller 包下的方法，是否为管理端操作在方法体内判断 */
    @Pointcut("execution(* com.ruyi.ruyi_mart..controller..*(..))")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        // 非 Web 上下文（如单元测试直接调 Service）或无需审计的请求，直接放行
        if (request == null || !shouldRecord(joinPoint, request)) {
            return joinPoint.proceed();
        }

        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            try {
                saveLog(joinPoint, request, error, System.currentTimeMillis() - start);
            } catch (Exception e) {
                // 写日志失败绝不能影响业务主流程，只告警
                log.error("写入操作日志失败", e);
            }
        }
    }

    /** 三重判断：是否管理端接口 + 是否写操作 */
    private boolean shouldRecord(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return requiresAdmin(joinPoint);
    }

    /** 方法或类上标注了含 ADMIN 的 @PreAuthorize 即视为管理端操作 */
    private boolean requiresAdmin(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        PreAuthorize onMethod = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
        if (onMethod != null) {
            return onMethod.value().contains("ADMIN");
        }
        // 方法上没有则看类上（如 @PreAuthorize("hasRole('ADMIN')") 标在类级别）
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(joinPoint.getTarget());
        PreAuthorize onClass = AnnotatedElementUtils.findMergedAnnotation(targetClass, PreAuthorize.class);
        return onClass != null && onClass.value().contains("ADMIN");
    }

    private void saveLog(ProceedingJoinPoint joinPoint, HttpServletRequest request, Throwable error, long costMs) {
        OperationLog entity = new OperationLog();
        fillOperator(entity);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 模块/操作：优先取 @OpLog 注解，没有则按路径与 HTTP 方法推断
        OpLog opLog = AnnotatedElementUtils.findMergedAnnotation(method, OpLog.class);
        if (opLog == null) {
            opLog = AnnotatedElementUtils.findMergedAnnotation(
                    AopProxyUtils.ultimateTargetClass(joinPoint.getTarget()), OpLog.class);
        }
        entity.setModule(resolveModule(opLog, request));
        entity.setAction(resolveAction(opLog, method, request));

        entity.setRequestUri(request.getRequestURI());
        entity.setRequestMethod(request.getMethod());
        entity.setClassMethod(signature.getDeclaringType().getSimpleName() + "#" + method.getName());
        entity.setParams(buildParams(joinPoint, method));
        entity.setIp(resolveIp(request));
        entity.setCostMs(costMs);
        entity.setCreateTime(LocalDateTime.now());

        if (error == null) {
            entity.setSuccess(1);
        } else {
            entity.setSuccess(0);
            entity.setErrorMsg(truncate(error.getClass().getSimpleName() + ": " + error.getMessage(), ERROR_MAX_LENGTH));
        }
        operationLogMapper.insert(entity);
    }

    /** 操作人：principal 存的是 userId（见 JwtAuthenticationFilter），username 查库冗余进来 */
    private void fillOperator(OperationLog entity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            return;
        }
        entity.setUserId(userId);
        User user = userMapper.selectById(userId);
        if (user != null) {
            entity.setUsername(user.getUsername());
        }
    }

    private String resolveModule(OpLog opLog, HttpServletRequest request) {
        if (opLog != null && !opLog.module().isEmpty()) {
            return opLog.module();
        }
        // 路径形如 /admin/order/page 或 /product，取最后一段 admin 之后的首段
        String uri = request.getRequestURI();
        String[] segments = uri.split("/");
        for (int i = 0; i < segments.length; i++) {
            if ("admin".equals(segments[i]) && i + 1 < segments.length) {
                return MODULE_NAMES.getOrDefault(segments[i + 1], segments[i + 1]);
            }
        }
        for (String segment : segments) {
            if (MODULE_NAMES.containsKey(segment)) {
                return MODULE_NAMES.get(segment);
            }
        }
        return "其他";
    }

    private String resolveAction(OpLog opLog, Method method, HttpServletRequest request) {
        if (opLog != null && !opLog.action().isEmpty()) {
            return opLog.action();
        }
        return switch (request.getMethod().toUpperCase()) {
            case "POST" -> "新增/操作";
            case "PUT" -> "修改";
            case "PATCH" -> "修改";
            case "DELETE" -> "删除";
            default -> method.getName();
        };
    }

    /**
     * 参数序列化为 JSON，并做脱敏与截断。
     * MultipartFile 不序列化（内容无意义且极大），只记文件名。
     */
    private String buildParams(ProceedingJoinPoint joinPoint, Method method) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        String[] names = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof HttpServletRequest || arg instanceof jakarta.servlet.http.HttpServletResponse) {
                continue;
            }
            String name = (names != null && i < names.length && names[i] != null) ? names[i] : ("arg" + i);
            if (arg instanceof MultipartFile file) {
                map.put(name, file.getOriginalFilename());
            } else if (isSensitive(name)) {
                map.put(name, "******");
            } else {
                map.put(name, arg);
            }
        }
        try {
            return truncate(objectMapper.writeValueAsString(map), PARAMS_MAX_LENGTH);
        } catch (Exception e) {
            return truncate(Arrays.toString(args), PARAMS_MAX_LENGTH);
        }
    }

    private boolean isSensitive(String name) {
        String lower = name.toLowerCase();
        for (String key : SENSITIVE_KEYS) {
            if (lower.contains(key.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /** 反向代理场景优先取 X-Forwarded-For 的第一段（真实客户端IP） */
    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        String remote = request.getRemoteAddr();
        // 本机访问时 Servlet 容器返回的是 IPv6 回环地址，转成更易读的 127.0.0.1
        return "0:0:0:0:0:0:0:1".equals(remote) || "::1".equals(remote) ? "127.0.0.1" : remote;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
