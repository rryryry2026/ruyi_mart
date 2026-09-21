package com.ruyi.ruyi_mart.module.user.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.user.dto.LoginRequest;
import com.ruyi.ruyi_mart.module.user.dto.LoginResponse;
import com.ruyi.ruyi_mart.module.user.dto.RefreshRequest;
import com.ruyi.ruyi_mart.module.user.service.impl.LoginServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class LoginController {

    @Autowired
    private LoginServiceImpl loginServiceImpl;

    /**用户登录*/
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req){
        LoginResponse resp = loginServiceImpl.login(req);
        return Result.success(resp);
    }

    /**用户重续令牌*/
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody RefreshRequest req){
        LoginResponse resp = loginServiceImpl.refresh(req.getRefreshToken());
        return Result.success(resp);
    }

    /**用户登出*/
    @PostMapping("/logout")
    public Result<Void> logout(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        loginServiceImpl.logout(userId);
        return Result.success();
    }


}
