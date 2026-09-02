package com.ruyi.ruyi_mart.module.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.user.dto.UpdateProfileDTO;
import com.ruyi.ruyi_mart.module.user.entity.User;
import com.ruyi.ruyi_mart.module.user.mapper.UserMapper;
import com.ruyi.ruyi_mart.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword){
        User user = baseMapper.selectById(userId);
        if(user == null){
            throw new BusinessException(ResultCode.NOT_FIND, "用户不存在");
        }
        if(!passwordEncoder.matches(oldPassword, user.getPassword())){
            throw new BusinessException(ResultCode.FAIL, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileDTO dto){
        User user = baseMapper.selectById(userId);
        if(user == null){
            throw new BusinessException(ResultCode.NOT_FIND, "用户不存在");
        }
        if(dto.getNickname() == null && dto.getPhone() == null){
            throw new BusinessException(ResultCode.FAIL, "至少填写一项");
        }

        if(dto.getNickname() != null){
            user.setNickname(dto.getNickname());
        }
        if(dto.getPhone() != null){
            user.setPhone(dto.getPhone());
        }
        user.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(user);
    }
}
