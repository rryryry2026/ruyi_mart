package com.ruyi.ruyi_mart.module.coupon.controller;


import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponReceiveDTO;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponUseDTO;
import com.ruyi.ruyi_mart.module.coupon.service.CouponUserService;
import com.ruyi.ruyi_mart.module.coupon.vo.CouponTemplateVO;
import com.ruyi.ruyi_mart.module.coupon.vo.CouponUserVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    /**领券中心：当前用户还能领的券（服务端已按领取上限、库存、互斥组预筛）*/
    @GetMapping("/receivable")
    public Result<List<CouponTemplateVO>> receivable() {
        return Result.success(couponUserService.listReceivable(currentUserId()));
    }

    /**我的券包（按使用状态可选筛选，分页）*/
    @GetMapping("/my")
    public Result<?> myCoupons(@RequestParam(required = false) Integer useStatus,
                               @RequestParam(required = false, defaultValue = "1") Integer page,
                               @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(couponUserService.myCoupons(currentUserId(), useStatus, page, size));
    }

    /**
     * 结算可用券列表（未用且在有效期内）。
     * 传 orderAmount 时会按订单金额算出每张券的抵扣额，并滤掉不满足门槛的券。
     */
    @GetMapping("/available")
    public Result<List<CouponUserVO>> available(@RequestParam(required = false) BigDecimal orderAmount) {
        return Result.success(couponUserService.listAvailable(currentUserId(), orderAmount));
    }

    /**结算用券（核销，返回本次抵扣金额）*/
    @PostMapping("/use")
    public Result<java.math.BigDecimal> use(@Valid @RequestBody CouponUseDTO dto) {
        java.math.BigDecimal discount = couponUserService.useCoupon(currentUserId(), dto);
        return Result.success(discount);
    }
}
