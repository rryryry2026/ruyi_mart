package com.ruyi.ruyi_mart.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.user.entity.User;

public interface UserService extends IService<User> {

    void register(User user);
}
