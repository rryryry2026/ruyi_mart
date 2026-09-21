package com.ruyi.ruyi_mart.security.config;

import org.springframework.http.HttpMethod;
import com.ruyi.ruyi_mart.security.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 安全总开关。职责：跨域 ，授权 ，装配 。
 * 负责拦人。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**cors跨域配置。*/
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        //允许哪些来源。支持通配符 + 凭证共存。
        config.setAllowedOriginPatterns(List.of("*"));
        //允许哪些方法。
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        //允许哪些请求头。
        config.setAllowedHeaders(List.of("*"));
        //允许携带凭证。
        config.setAllowCredentials(true);
        //预检结果缓存 3600 秒，减少重复预检。
        config.setMaxAge(3600L);
        //把上面的规则绑到路径上。
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                //接入 CORS：让上面的corsConfigurationSource生效。
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                //关闭CSRF防护。
                .csrf(csrf -> csrf.disable())
                //不使用session。
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //授权规则清单。
                .authorizeHttpRequests(auth -> auth
                        //放行浏览器OPTIONS预检请求，否则预检会被拦截
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        //免登录。
                        .requestMatchers("/user/login", "/user/register", "/user/refresh").permitAll()
                        //游客可浏览的开放接口。
                        .requestMatchers("/banner/**").permitAll()
                        .requestMatchers("/category/**").permitAll()
                        .requestMatchers("/product/**").permitAll()
                        .requestMatchers("/stock/**").permitAll()
                        //评价：列表、回复列表、追评、评论数都是只读的，游客可看。
                        //注意 /review/first/append 是"发追评"，属于写操作，不在放行之列。
                        .requestMatchers("/review/first/page", "/review/second/page",
                                "/review/count", "/review/append").permitAll()
                        // 公告/热搜词。
                        .requestMatchers("/notice/**").permitAll()
                        .requestMatchers("/hot-keyword/**").permitAll()
                        // 购物车（游客可通过 X-Guest-Id 访问）。
                        .requestMatchers("/cart/**").permitAll()
                        // 支付回调类接口。
                        .requestMatchers("/payment/mock/confirm", "/payment/alipay/notify", "/payment/wechat/notify").permitAll()
                        // Swagger / API 文档
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error").permitAll()
                        //其余全部要登录。
                        .anyRequest().authenticated()
                )
                //把jwt过滤器塞进认证阶段第一位。
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                //未认证返回。
                .exceptionHandling(ex -> ex
                                .authenticationEntryPoint((request, response, authException) -> {
                                    response.setStatus(401);
                                    response.setContentType("application/json;charset=UTF-8");
                                    response.getWriter().write("{\"code\":401,\"message\":\"未认证，请先登录\"}");
                                })
                        );

        return http.build();
    }
}
