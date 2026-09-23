package com.ruyi.ruyi_mart.module.coupon.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponCreateDTO;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponQueryDTO;
import com.ruyi.ruyi_mart.module.coupon.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**优惠券的管理端接口。*/
@RestController
@RequestMapping("/admin/coupon")
@PreAuthorize("hasRole('ADMIN')")
public class CouponAdminController {

    @Autowired
    private CouponService couponService;

    /**新建优惠券*/
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CouponCreateDTO dto) {
        couponService.saveCouponAdmin(dto);
        return Result.success();
    }

    /**分页查询优惠券列表（活动名称/类型/状态可选筛选）*/
    @GetMapping("/page")
    public Result<?> pageList(CouponQueryDTO dto) {
        return Result.success(couponService.listCoupons(dto));
    }

    /**优惠券详情（管理端编辑回显用）*/
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(couponService.getById(id));
    }

    /**修改优惠券*/
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CouponCreateDTO dto) {
        couponService.updateCoupon(id, dto);
        return Result.success();
    }

    /**删除优惠券（物理删除）*/
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return Result.success();
    }

    /**启停 / 修改模板状态*/
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        couponService.updateStatus(id, status);
        return Result.success();
    }
}
