package com.ruyi.ruyi_mart.security.filter;

import com.ruyi.ruyi_mart.common.util.JwtUtil;
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
                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);
                Integer userType = jwtUtil.getUserType(token);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if(userType != null && userType == 1){
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId,null,authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("JWT鉴权成功:userId={}, username={}",userId,username);
            }catch (Exception e){
                log.debug("JWT鉴权失败:{}",e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request,response);
    }

    private String resolveToken(HttpServletRequest request){
        String header = request.getHeader(AUTH_HEADER);
        if(StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)){
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
