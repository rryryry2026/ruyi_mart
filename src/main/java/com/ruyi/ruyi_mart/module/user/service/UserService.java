package com.ruyi.ruyi_mart.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.user.dto.UpdateProfileDTO;
import com.ruyi.ruyi_mart.module.user.entity.User;

public interface UserService extends IService<User> {

    void register(User user);

    /** 修改密码*/
    void changePassword(Long userId, String oldPassword, String newPassword);

    /** 修改个人资料 */
    void updateProfile(Long userId, UpdateProfileDTO dto);
}
