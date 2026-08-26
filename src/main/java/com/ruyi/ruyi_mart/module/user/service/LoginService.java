package com.ruyi.ruyi_mart.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.ruyi_mart.common.config.JwtProperties;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.common.util.JwtUtil;
import com.ruyi.ruyi_mart.module.user.dto.LoginRequest;
import com.ruyi.ruyi_mart.module.user.dto.LoginResponse;
import com.ruyi.ruyi_mart.module.user.entity.User;
import com.ruyi.ruyi_mart.module.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LoginService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private RedissonClient redissonClient;

    public LoginResponse login(LoginRequest req){

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername,req.getUsername())
        );

        if(user == null){
            throw new BusinessException(ResultCode.FAIL,"用户名或密码错误");
        }

        if(!passwordEncoder.matches(req.getPassword(), user.getPassword())){
            throw new BusinessException(ResultCode.FAIL,"用户名或密码错误");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        String redisKey = "ruyi:user:refresh:" + user.getId();
        redissonClient.getBucket(redisKey)
                .set(refreshToken,jwtProperties.getRefreshExpire(), TimeUnit.SECONDS);
        LoginResponse resp = new LoginResponse();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setExpiresIn(jwtProperties.getAccessExpire());
        return resp;

    }


}
