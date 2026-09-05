package com.ruyi.ruyi_mart.module.coupon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponCreateDTO;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponQueryDTO;
import com.ruyi.ruyi_mart.module.coupon.entity.Coupon;

public interface CouponService extends IService<Coupon> {

    /**管理端分页列表*/
    IPage<Coupon> listCoupons(CouponQueryDTO dto);

    /**管理端创建券*/
    void saveCouponAdmin(CouponCreateDTO dto);

    /**管理端更新券*/
    void updateCoupon(Long id, CouponCreateDTO dto);

    /**管理端删除券（基础版物理删除）*/
    void deleteCoupon(Long id);

    /**管理端启停 / 改模板状态*/
    void updateStatus(Long id, Integer status);
}
