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

@Slf4j
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private final SecretKey key;

    @Autowired
    public JwtUtil(JwtProperties jwtProperties){
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId,String username,Integer userType){
        return buildToken(userId,username,userType,jwtProperties.getAccessExpire());
    }

    public String generateRefreshToken(Long userId,String username,Integer userType){
        return buildToken(userId,username,userType,jwtProperties.getRefreshExpire());
    }

    private String buildToken(Long userId,String username,Integer userType,Long expireSeconds){
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .subject(username)
                .claim("uid",userId)
                .claim("userType", userType)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token){
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token){
        try {
            parseToken(token);
            return true;
        }catch (Exception e){
            log.debug("JWT校验失败：{}",e.getMessage());
            return false;
        }
    }

    public Long getUserId(String token){

        return  parseToken(token).get("uid", Long.class);
    }

    public String getUsername(String token){

        return parseToken(token).getSubject();
    }

    public Integer getUserType(String token){
        return parseToken(token).get("userType", Integer.class);
    }
}
