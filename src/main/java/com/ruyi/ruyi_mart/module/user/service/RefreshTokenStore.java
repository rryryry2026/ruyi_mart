package com.ruyi.ruyi_mart.module.user.service;

import com.ruyi.ruyi_mart.common.config.JwtProperties;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * refreshToken 的存取。
 *
 * Redis key 的拼法只保留在这一处：登录、续期、登出、改密码都要读写它，
 * 散在多个类里早晚会有一处拼错，而拼错的那处是静默失效的
 * —— 表现为"改了密码却踢不掉旧令牌"这类查起来很费劲的问题。
 */
@Component
public class RefreshTokenStore {

    /** 一个用户一条：key = ruyi:user:refresh:{userId}，value = refreshToken */
    private static final String KEY_PREFIX = "ruyi:user:refresh:";

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private JwtProperties jwtProperties;

    /** 登录时写入，过期时间与 refreshToken 的有效期一致 */
    public void save(Long userId, String refreshToken){
        redissonClient.getBucket(key(userId))
                .set(refreshToken, jwtProperties.getRefreshExpire(), TimeUnit.SECONDS);
    }

    /** 取当前保存的 refreshToken，没有则返回 null */
    public String get(Long userId){
        return (String) redissonClient.getBucket(key(userId)).get();
    }

    /** 登出、改密码、禁用账号时调用，让"续期"这条路立刻失效 */
    public void remove(Long userId){
        redissonClient.getBucket(key(userId)).delete();
    }

    private String key(Long userId){
        return KEY_PREFIX + userId;
    }
}
