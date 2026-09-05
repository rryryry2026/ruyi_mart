package com.ruyi.ruyi_mart.module.coupon.controller;


import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponReceiveDTO;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponUseDTO;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponUser;
import com.ruyi.ruyi_mart.module.coupon.service.CouponUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupon")

/**优惠券用户端控制器*/

public class CouponUserController {

    @Autowired
    private CouponUserService couponUserService;

    /**从登录态取当前用户ID*/
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }

    /**用户领券*/
    @PostMapping("/receive")
    public Result<Void> receive(@Valid @RequestBody CouponReceiveDTO dto) {
        couponUserService.receiveCoupon(currentUserId(), dto);
        return Result.success();
    }

    /**我的券包（按使用状态可选筛选，分页）*/
    @GetMapping("/my")
    public Result<?> myCoupons(@RequestParam(required = false) Integer useStatus,
                               @RequestParam(required = false, defaultValue = "1") Integer page,
                               @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(couponUserService.myCoupons(currentUserId(), useStatus, page, size));
    }

    /**结算可用券列表（未用且在有效期内）*/
    @GetMapping("/available")
    public Result<List<CouponUser>> available() {
        return Result.success(couponUserService.listAvailable(currentUserId()));
    }

    /**结算用券（核销，返回本次抵扣金额）*/
    @PostMapping("/use")
    public Result<java.math.BigDecimal> use(@Valid @RequestBody CouponUseDTO dto) {
        java.math.BigDecimal discount = couponUserService.useCoupon(currentUserId(), dto);
        return Result.success(discount);
    }
}
