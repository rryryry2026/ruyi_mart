package com.ruyi.ruyi_mart.module.refund.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.refund.service.RefundService;
import com.ruyi.ruyi_mart.module.refund.vo.RefundAdminVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端退款接口：管理员视角查全量退款单（现有 /refund/list 只能查自己的）
 */
@RestController
@RequestMapping("/admin/refund")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRefundController {

    @Autowired
    private RefundService refundService;

    /**退款单分页（按状态筛选）*/
    @GetMapping("/page")
    public Result<Page<RefundAdminVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(refundService.adminPageRefunds(status, pageNum, pageSize));
    }
}
