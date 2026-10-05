package com.ruyi.ruyi_mart.module.refund.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.common.util.SecurityUtils;
import com.ruyi.ruyi_mart.module.log.annotation.OpLog;
import com.ruyi.ruyi_mart.module.refund.entity.Refund;
import com.ruyi.ruyi_mart.module.refund.service.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/refund")
public class RefundController {

    @Autowired
    private RefundService refundService;

    private Long currentUserId(){
        return SecurityUtils.currentUserId();
    }

    @PostMapping("/apply")
    public Result<Refund> apply(@RequestParam Long orderId,
                                @RequestParam String reason){
        return Result.success(refundService.apply(currentUserId(),orderId,reason));
    }

    @GetMapping("/list")
    public Result<List<Refund>> list(){
        return Result.success(refundService.listByUser(currentUserId()));
    }

    @GetMapping("/{id}")
    public Result<Refund> detail(@PathVariable Long id){
        return Result.success(refundService.detail(currentUserId(),id));
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OpLog(module = "退款审核", action = "同意退款")
    public Result<Refund> approve(@PathVariable Long id){
        return Result.success(refundService.approve(id));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OpLog(module = "退款审核", action = "拒绝退款")
    public Result<Refund> reject(@PathVariable Long id,
                                 @RequestParam String reason){
        return Result.success(refundService.reject(id, reason));
    }



}
