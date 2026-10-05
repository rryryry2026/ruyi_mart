package com.ruyi.ruyi_mart.common.util;


import com.ruyi.ruyi_mart.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**jwt令牌的签发和查验中心。*/
@Slf4j
@Component
public class JwtUtil {

    /**typ 声明的取值：访问令牌*/
    public static final String TOKEN_TYPE_ACCESS = "access";
    /**typ 声明的取值：续期令牌*/
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    private final SecretKey key;

    /**构造器注入，密钥字符串->加密用的SecretKey。*/
    @Autowired
    public JwtUtil(JwtProperties jwtProperties){
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**发短令牌。*/
    public String generateAccessToken(Long userId,String username,Integer userType){
        return buildToken(userId,username,userType,jwtProperties.getAccessExpire(),TOKEN_TYPE_ACCESS);
    }

    /**发长令牌。*/
    public String generateRefreshToken(Long userId,String username,Integer userType){
        return buildToken(userId,username,userType,jwtProperties.getRefreshExpire(),TOKEN_TYPE_REFRESH);
    }

    /**造令牌。*/
    private String buildToken(Long userId,String username,Integer userType,Long expireSeconds,String tokenType){
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .subject(username)
                .claim("uid",userId)
                .claim("userType", userType)
                // 令牌类型标记：两种令牌除有效期外完全一样，不标类型的话
                // 7 天有效的 refreshToken 可以直接当 accessToken 用，
                // "accessToken 只活 15 分钟"的收敛设计等于不存在
                .claim("typ", tokenType)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**验令牌+读内容。*/
    public Claims parseToken(String token){
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**判断令牌是否有效。*/
    public boolean validateToken(String token){
        try {
            parseToken(token);
            return true;
        }catch (Exception e){
            log.debug("JWT校验失败：{}",e.getMessage());
            return false;
        }
    }

    /**取用户Id。*/
    public Long getUserId(String token){

        return  parseToken(token).get("uid", Long.class);
    }

    /**取用户名。*/
    public String getUsername(String token){

        return parseToken(token).getSubject();
    }

    /**取用户类型。*/
    public Integer getUserType(String token){
        return parseToken(token).get("userType", Integer.class);
    }
}
