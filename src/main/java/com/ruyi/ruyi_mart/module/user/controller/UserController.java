package com.ruyi.ruyi_mart.module.user.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.common.util.SecurityUtils;
import com.ruyi.ruyi_mart.module.user.dto.ChangePasswordDTO;
import com.ruyi.ruyi_mart.module.user.dto.RegisterRequest;
import com.ruyi.ruyi_mart.module.user.dto.UpdateProfileDTO;
import com.ruyi.ruyi_mart.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**消费端：注册 / 改密码 / 改资料。管理端的用户管理在 /admin/user 下。*/
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**取当前登录用户ID；未登录（匿名）时由 SecurityUtils 抛 401*/
    private Long currentUserId(){
        return SecurityUtils.currentUserId();
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req){
        userService.register(req);
        return Result.success();
    }

    @PostMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto){
        userService.changePassword(currentUserId(), dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }

    @PostMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto){
        userService.updateProfile(currentUserId(),dto);
        return Result.success();
    }
}
