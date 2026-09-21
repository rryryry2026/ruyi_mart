package com.ruyi.ruyi_mart.module.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.user.dto.RegisterRequest;
import com.ruyi.ruyi_mart.module.user.dto.UpdateProfileDTO;
import com.ruyi.ruyi_mart.module.user.dto.UserAdminQueryDTO;
import com.ruyi.ruyi_mart.module.user.entity.User;
import com.ruyi.ruyi_mart.module.user.vo.UserAdminVO;

public interface UserService extends IService<User> {

    /** 注册：用户名查重、密码加密后落库 */
    void register(RegisterRequest req);

    /** 修改密码*/
    void changePassword(Long userId, String oldPassword, String newPassword);

    /** 修改个人资料 */
    void updateProfile(Long userId, UpdateProfileDTO dto);

    /** 管理端：用户分页（脱敏，不含密码字段）*/
    Page<UserAdminVO> adminPageUsers(UserAdminQueryDTO dto);

    /** 管理端：启用/禁用用户 */
    void updateUserStatus(Long userId, Integer status);
}
