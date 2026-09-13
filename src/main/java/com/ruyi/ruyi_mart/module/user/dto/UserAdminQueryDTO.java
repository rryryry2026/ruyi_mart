package com.ruyi.ruyi_mart.module.user.dto;

import lombok.Data;

/**
 * 管理端用户分页查询入参
 */
@Data
public class UserAdminQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**关键字：用户名/昵称/手机号 模糊匹配*/
    private String keyword;

    /**用户类型筛选：1=管理员，其他=普通用户*/
    private Integer userType;

    /**状态筛选：1=正常，0=禁用*/
    private Integer status;
}
