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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // CORS 配置源：声明哪些来源/方法/请求头允许跨域
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 开发期全放开；上线时把 "*" 换成前端真实域名，如 List.of("https://shop.xxx.com")
        config.setAllowedOriginPatterns(List.of("*"));
        // 允许的 HTTP 方法，必须包含 OPTIONS（浏览器预检用）
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许的请求头，前端会带 Authorization，所以用 *
        config.setAllowedHeaders(List.of("*"));
        // 允许携带凭证（Cookie / Token 头）；配合 originPatterns 使用，避免 "*" 与凭证冲突
        config.setAllowCredentials(true);
        // 预检结果缓存 3600 秒，减少重复预检
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                // ★ 接入 CORS：让上面的 corsConfigurationSource 生效
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ★ 放行浏览器 OPTIONS 预检请求，否则预检会被拦截
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 鉴权相关
                        .requestMatchers("/user/login", "/user/register", "/user/refresh").permitAll()
                        // 游客可浏览的开放接口（电商消费端核心）
                        .requestMatchers("/banner/**").permitAll()
                        .requestMatchers("/category/**").permitAll()
                        .requestMatchers("/product/**").permitAll()
                        .requestMatchers("/stock/**").permitAll()
                        .requestMatchers("/review/first/page", "/review/count", "/review/second/page").permitAll()
                        // 购物车（游客可通过 X-Guest-Id 访问）
                        .requestMatchers("/cart/**").permitAll()
                        // 支付回调类接口：模拟真实支付平台的异步通知，本身不带用户登录态，
                        // 由各自的 @PreAuthorize 限制为管理员调用。
                        // 这里刻意不写成 /payment/**：个人端的 /payment/mock/pay/{orderId}
                        // 必须落到下面的 authenticated()，否则任何人都能确认别人的订单。
                        .requestMatchers("/payment/mock/confirm", "/payment/alipay/notify", "/payment/wechat/notify").permitAll()
                        // Swagger / API 文档
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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
