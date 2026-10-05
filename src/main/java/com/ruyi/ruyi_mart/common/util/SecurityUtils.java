package com.ruyi.ruyi_mart.common.util;

import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 Spring Security 上下文取当前登录用户的工具。
 * 各 Controller 不再各自手写取值逻辑：principal 存的是 userId（见 JwtAuthenticationFilter）。
 */
public final class SecurityUtils {

    /**纯静态工具类，不实例化。*/
    private SecurityUtils() {
    }

    /**
     * 当前登录用户 ID。匿名/未认证时抛 401：
     * 各接口此前有"裸强转 (Long) principal"和"判空返回 null"两种写法，
     * 前者匿名访问直接 500，后者把判空责任散给了每个调用方，这里统一收口。
     */
    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
