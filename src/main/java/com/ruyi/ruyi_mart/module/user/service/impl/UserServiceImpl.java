package com.ruyi.ruyi_mart.module.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.user.entity.User;
import com.ruyi.ruyi_mart.module.user.mapper.UserMapper;
import com.ruyi.ruyi_mart.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void register(User user){
        long count = this.lambdaQuery()
                .eq(User::getUsername,user.getUsername())
                .count();
        if(count > 0){
            throw new BusinessException(ResultCode.FAIL,"用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setUserType(2);
        this.save(user);
    }
}
