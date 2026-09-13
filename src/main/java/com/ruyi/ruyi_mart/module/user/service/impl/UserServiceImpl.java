package com.ruyi.ruyi_mart.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.user.dto.UpdateProfileDTO;
import com.ruyi.ruyi_mart.module.user.dto.UserAdminQueryDTO;
import com.ruyi.ruyi_mart.module.user.entity.User;
import com.ruyi.ruyi_mart.module.user.mapper.UserMapper;
import com.ruyi.ruyi_mart.module.user.service.UserService;
import com.ruyi.ruyi_mart.module.user.vo.UserAdminVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public Page<UserAdminVO> adminPageUsers(UserAdminQueryDTO dto){
        Page<User> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if(StringUtils.hasText(dto.getKeyword())){
            // 用 and(...) 把三个 OR 条件包成一组括号，
            // 否则会和后面的类型/状态条件混在一起，变成 "类型=x AND 用户名 LIKE y OR 昵称 LIKE y" 的错误优先级
            qw.and(w -> w.like(User::getUsername, dto.getKeyword())
                    .or().like(User::getNickname, dto.getKeyword())
                    .or().like(User::getPhone, dto.getKeyword()));
        }
        if(dto.getUserType() != null){
            qw.eq(User::getUserType, dto.getUserType());
        }
        if(dto.getStatus() != null){
            qw.eq(User::getStatus, dto.getStatus());
        }
        qw.orderByDesc(User::getCreateTime);
        this.page(page, qw);

        Page<UserAdminVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<UserAdminVO> vos = new ArrayList<>();
        for(User u : page.getRecords()){
            UserAdminVO vo = new UserAdminVO();
            // 逐个字段手工搬运，杜绝把 password 带出去
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setPhone(u.getPhone());
            vo.setUserType(u.getUserType());
            vo.setStatus(u.getStatus());
            vo.setCreateTime(u.getCreateTime());
            vo.setUpdateTime(u.getUpdateTime());
            vos.add(vo);
        }
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status){
        if(status == null || (status != 0 && status != 1)){
            throw new BusinessException(ResultCode.FAIL, "status 必须为 0 或 1");
        }
        User user = baseMapper.selectById(userId);
        if(user == null){
            throw new BusinessException(ResultCode.NOT_FIND, "用户不存在");
        }
        // 防止把管理员自己禁掉导致后台无人能进
        if(user.getUserType() != null && user.getUserType() == 1){
            throw new BusinessException(ResultCode.FAIL, "管理员账号不可禁用");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(user);
    }
}
