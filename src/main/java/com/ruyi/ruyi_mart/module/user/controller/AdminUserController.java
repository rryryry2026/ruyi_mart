package com.ruyi.ruyi_mart.module.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.user.dto.UserAdminQueryDTO;
import com.ruyi.ruyi_mart.module.user.service.UserService;
import com.ruyi.ruyi_mart.module.user.vo.UserAdminVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户接口。
 * 原有 GET /user/list 返回裸 User 实体（含密码哈希）且无分页，这里提供脱敏分页版本。
 */
@RestController
@RequestMapping("/admin/user")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private UserService userService;

    /**用户分页（关键字/类型/状态筛选，返回不含密码字段的 VO）*/
    @GetMapping("/page")
    public Result<Page<UserAdminVO>> page(UserAdminQueryDTO dto) {
        return Result.success(userService.adminPageUsers(dto));
    }

    /**启用/禁用用户（禁用后无法登录，已登录的 token 到期后失效）*/
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        return Result.success();
    }
}
