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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**用户登录服务。*/
@Slf4j
@Service
public class LoginService {

    @Autowired
    private UserMapper userMapper;//查用户表
    @Autowired
    private PasswordEncoder passwordEncoder;//BCrypt工具
    @Autowired
    private JwtUtil jwtUtil;//签令牌、验令牌、解字段
    @Autowired
    private JwtProperties jwtProperties;//jwt配置项
    @Autowired
    private RefreshTokenStore refreshTokenStore;//refreshToken 的 Redis 存取

    /**用户登录：验账号密码，签发两张令牌，并把 refreshToken 记进 Redis*/
    public LoginResponse login(LoginRequest req){

        //用户名查人
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername,req.getUsername())
        );
        if(user == null){
            throw new BusinessException(ResultCode.UNAUTHORIZED,"用户名或密码错误");
        }

        //密码比对。两种失败给同一句提示，防账号枚举
        if(!passwordEncoder.matches(req.getPassword(), user.getPassword())){
            throw new BusinessException(ResultCode.UNAUTHORIZED,"用户名或密码错误");
        }

        // 被管理员禁用的账号不允许登录（status: 1=正常 0=禁用，null 视为正常）
        if(user.getStatus() != null && user.getStatus() == 0){
            throw new BusinessException(ResultCode.FORBIDDEN,"账号已被禁用，请联系管理员");
        }

        //签发两张令牌
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(),user.getUserType());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(),user.getUserType());
        refreshTokenStore.save(user.getId(), refreshToken);

        return buildResponse(user.getId(), user.getUsername(), user.getNickname(), accessToken, refreshToken);
    }

    /**续期：拿 refreshToken 换一张新的 accessToken*/
    public LoginResponse refresh(String refreshToken){
        //验签
        if(!jwtUtil.validateToken(refreshToken)){
            throw new BusinessException(ResultCode.UNAUTHORIZED,"refreshToken 无效或已过期");
        }

        //只从令牌里取 userId（用来定位用户），身份信息一律以数据库为准
        Long userId = jwtUtil.getUserId(refreshToken);

        //与 Redis 里保存的那张比对：登出、改密码、禁用之后都会被删掉，这里就换不到新令牌
        if(!refreshToken.equals(refreshTokenStore.get(userId))){
            throw new BusinessException(ResultCode.UNAUTHORIZED,"refreshToken 已失效，请重新登录");
        }

        //复核账号是否还在、有没有被禁用
        User current = userMapper.selectById(userId);
        if(current == null){
            throw new BusinessException(ResultCode.UNAUTHORIZED,"用户不存在，请重新登录");
        }
        if(current.getStatus() != null && current.getStatus() == 0){
            refreshTokenStore.remove(userId);
            throw new BusinessException(ResultCode.FORBIDDEN,"账号已被禁用，请联系管理员");
        }

        // 新令牌用的是【库里当前】的用户名和身份，不能沿用旧令牌里解出来的那份。
        // 令牌里的值是签发那一刻的快照：改过用户名、或被撤销了管理员权限之后，
        // 拿旧 refreshToken 续期会把旧身份原样续回来
        // —— 管理员权限被撤销却最多要等令牌过期才生效，问题就出在这里。
        String newAccessToken = jwtUtil.generateAccessToken(
                current.getId(), current.getUsername(), current.getUserType());

        return buildResponse(current.getId(), current.getUsername(), current.getNickname(), newAccessToken, refreshToken);
    }

    /**用户登出：把 Redis 里的 refreshToken 删掉，让续期能力失效*/
    public void logout(Long userId){
        refreshTokenStore.remove(userId);
    }

    /**组装登录/续期的返回体*/
    private LoginResponse buildResponse(Long userId, String username, String nickname,
                                        String accessToken, String refreshToken){
        LoginResponse resp = new LoginResponse();
        resp.setUserId(userId);
        resp.setUsername(username);
        resp.setNickname(nickname);
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setExpiresIn(jwtProperties.getAccessExpire());
        return resp;
    }
}
