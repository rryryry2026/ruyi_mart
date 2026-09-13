package com.ruyi.ruyi_mart.module.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户视图。
 * 刻意"不继承" User 实体：User 里有 password 字段，一旦继承并对 User 对象做
 * BeanUtils 复制或直接序列化，密码哈希就会随接口返回给前端。这里平铺所需字段，杜绝泄漏。
 */
@Data
public class UserAdminVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    /**1=管理员，其余为普通用户*/
    private Integer userType;

    /**1=正常，0=禁用*/
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
