package com.ruyi.ruyi_mart.security.filter;

import com.ruyi.ruyi_mart.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * jwt过滤器，进入controller之前验token，记身份。
 * 职责：认人，不是拦人。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException{
        String token = resolveToken(request);

        if(StringUtils.hasText(token) && jwtUtil.validateToken(token)){
            try{
                // 只解析这一次：原来 getUserId/getUsername/getUserType 各 parse 一遍，
                // 一个请求重复验签 4 次，纯浪费
                Claims claims = jwtUtil.parseToken(token);
                // refreshToken 只用于换新令牌（/user/refresh），不能当 accessToken 认证用
                if(!JwtUtil.TOKEN_TYPE_REFRESH.equals(claims.get("typ", String.class))){
                    Long userId = claims.get("uid", Long.class);
                    Integer userType = claims.get("userType", Integer.class);

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if(userType != null && userType == 1){
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId,null,authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("JWT鉴权成功:userId={}, username={}",userId,claims.getSubject());
                }

            }catch (Exception e){
                log.debug("JWT鉴权失败:{}",e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request,response);
    }

    /**去掉bearer 前缀，取出token。*/
    private String resolveToken(HttpServletRequest request){
        String header = request.getHeader(AUTH_HEADER);
        if(StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)){
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
