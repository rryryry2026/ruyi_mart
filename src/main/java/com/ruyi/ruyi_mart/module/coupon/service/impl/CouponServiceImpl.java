package com.ruyi.ruyi_mart.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponCreateDTO;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponQueryDTO;
import com.ruyi.ruyi_mart.module.coupon.entity.Coupon;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponMapper;
import com.ruyi.ruyi_mart.module.coupon.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Wrapper;
import java.time.LocalDateTime;

@Service
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    /**管理端分页列表*/
    @Override
    public IPage<Coupon> listCoupons(CouponQueryDTO dto){
        Page<Coupon> page = new Page<>(dto.getPage(),dto.getSize());
        return page(page, Wrappers.<Coupon>lambdaQuery()
                .like(StringUtils.hasText(dto.getActivityName()),Coupon::getActivityName,dto.getActivityName())
                        .eq(dto.getCouponType() != null,Coupon::getCouponType,dto.getCouponType())
                        .eq(dto.getStatus() != null,Coupon::getStatus,dto.getStatus())
                        .orderByDesc(Coupon::getCreateTime));

    }

    /**管理端创建券*/
    @Override
    public void saveCouponAdmin(CouponCreateDTO dto) {
        Coupon coupon = new Coupon();
        coupon.setActivityName(dto.getActivityName());
        coupon.setCouponType(dto.getCouponType());
        coupon.setFaceValue(dto.getFaceValue());
        coupon.setDiscountRate(dto.getDiscountRate());
        coupon.setMaxDiscount(dto.getMaxDiscount());
        coupon.setMinSpend(dto.getMinSpend());
        coupon.setTotalQuota(dto.getTotalQuota() == null ? 0 : dto.getTotalQuota());
        coupon.setReceiveQuota(0);
        coupon.setUsedQuota(0);
        coupon.setValidMode(dto.getValidMode());
        coupon.setValidStart(dto.getValidStart());
        coupon.setValidEnd(dto.getValidEnd());
        coupon.setReceiveValidDays(dto.getReceiveValidDays());
        coupon.setLimitPerPerson(dto.getLimitPerPerson() == null ? 1 : dto.getLimitPerPerson());
        coupon.setUserLimitType(dto.getUserLimitType() == null ? 1 : dto.getUserLimitType());
        coupon.setUseScope(dto.getUseScope() == null ? 1 : dto.getUseScope());
        coupon.setMutexGroupCode(dto.getMutexGroupCode() == null ? 0L : dto.getMutexGroupCode());
        coupon.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        coupon.setCreateTime(LocalDateTime.now());
        coupon.setUpdateTime(LocalDateTime.now());
        save(coupon);
    }

    /**管理端更新券*/
    @Override
    public void updateCoupon(Long id, CouponCreateDTO dto){
        Coupon coupon = getById(id);
        if(coupon == null){
            throw new RuntimeException("优惠券不存在: " + id);
        }
        coupon.setActivityName(dto.getActivityName());
        coupon.setCouponType(dto.getCouponType());
        coupon.setFaceValue(dto.getFaceValue());
        coupon.setDiscountRate(dto.getDiscountRate());
        coupon.setMaxDiscount(dto.getMaxDiscount());
        coupon.setMinSpend(dto.getMinSpend());
        coupon.setTotalQuota(dto.getTotalQuota() == null ? 0 : dto.getTotalQuota());
        coupon.setValidMode(dto.getValidMode());
        coupon.setValidStart(dto.getValidStart());
        coupon.setValidEnd(dto.getValidEnd());
        coupon.setReceiveValidDays(dto.getReceiveValidDays());
        coupon.setLimitPerPerson(dto.getLimitPerPerson() == null ? 1 : dto.getLimitPerPerson());
        coupon.setUserLimitType(dto.getUserLimitType() == null ? 1 : dto.getUserLimitType());
        coupon.setUseScope(dto.getUseScope() == null ? 1 : dto.getUseScope());
        coupon.setMutexGroupCode(dto.getMutexGroupCode() == null ? 0L : dto.getMutexGroupCode());
        coupon.setStatus(dto.getStatus() == null ? coupon.getStatus() : dto.getStatus());
        coupon.setUpdateTime(LocalDateTime.now());
        updateById(coupon);
    }

    /**管理端删除券（物理删除）*/
    @Override
    public void deleteCoupon(Long id) {
        removeById(id);
    }

    /**管理端启停 / 改模板状态*/
    @Override
    public void updateStatus(Long id, Integer status) {
        lambdaUpdate()
                .eq(Coupon::getId, id)
                .set(Coupon::getStatus, status)
                .set(Coupon::getUpdateTime, LocalDateTime.now())
                .update();
    }


}
