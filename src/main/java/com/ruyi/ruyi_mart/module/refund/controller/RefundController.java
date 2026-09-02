package com.ruyi.ruyi_mart.module.refund.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.refund.entity.Refund;
import com.ruyi.ruyi_mart.module.refund.service.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/refund")
public class RefundController {

    @Autowired
    private RefundService refundService;

    private Long currentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
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
    public Result<Refund> approve(@PathVariable Long id){
        return Result.success(refundService.approve(id));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Refund> reject(@PathVariable Long id,
                                 @RequestParam String reason){
        return Result.success(refundService.reject(id, reason));
    }



}
