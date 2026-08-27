package com.ruyi.ruyi_mart.module.user.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.user.dto.LoginRequest;
import com.ruyi.ruyi_mart.module.user.dto.LoginResponse;
import com.ruyi.ruyi_mart.module.user.dto.RefreshRequest;
import com.ruyi.ruyi_mart.module.user.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.Authenticator;

@RestController
@RequestMapping("/user")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req){
        LoginResponse resp = loginService.login(req);
        return Result.success(resp);
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody RefreshRequest req){
        LoginResponse resp = loginService.refresh(req.getRefreshToken());
        return Result.success(resp);
    }

    @PostMapping("/logout")
    public Result<Void> logout(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        loginService.logout(userId);
        return Result.success();
    }


}
