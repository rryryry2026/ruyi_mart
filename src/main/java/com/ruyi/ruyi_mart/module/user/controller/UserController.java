package com.ruyi.ruyi_mart.module.user.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.user.dto.ChangePasswordDTO;
import com.ruyi.ruyi_mart.module.user.dto.RegisterRequest;
import com.ruyi.ruyi_mart.module.user.dto.UpdateProfileDTO;
import com.ruyi.ruyi_mart.module.user.entity.User;
import com.ruyi.ruyi_mart.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    private Long currentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req){
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(req.getPassword());
        user.setNickname(req.getNickname());
        user.setPhone(req.getPhone());
        userService.register(user);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<User>> list(){
        return Result.success(userService.list());
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
