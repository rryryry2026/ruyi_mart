package com.ruyi.ruyi_mart.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponReceiveDTO;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponUseDTO;
import com.ruyi.ruyi_mart.module.coupon.entity.Coupon;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponOrderRel;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponUser;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponTypeEnum;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponUseStatusEnum;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponValidModeEnum;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponMapper;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponOrderRelMapper;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponScopeDetailMapper;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponUserMapper;
import com.ruyi.ruyi_mart.module.coupon.service.CouponService;
import com.ruyi.ruyi_mart.module.coupon.service.CouponUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CouponUserServiceImpl extends ServiceImpl<CouponUserMapper, CouponUser> implements CouponUserService {

    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private CouponScopeDetailMapper couponScopeDetailMapper;
    @Autowired
    private CouponOrderRelMapper couponOrderRelMapper;

    /**用户领券*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveCoupon(Long userId, CouponReceiveDTO dto){
        Coupon coupon = couponService.getById(dto.getCouponId());
        if(coupon == null){
            throw new RuntimeException("优惠券不存在");
        }
        if(coupon.getStatus() == null || coupon.getStatus() != 1){
            throw new RuntimeException("优惠券不在发放中");
        }
        if(coupon.getTotalQuota() != null && coupon.getTotalQuota() > 0){
            if(coupon.getReceiveQuota() >= coupon.getTotalQuota()){
                throw new RuntimeException("优惠券已领完");
            }
        }

        long owned = count(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getCouponId, coupon.getId()));
        int limit = coupon.getLimitPerPerson() == null ? 1 : coupon.getLimitPerPerson();
        if (owned >= limit) {
            throw new RuntimeException("已达到单人领取上限");
        }

        if (coupon.getMutexGroupCode() != null && coupon.getMutexGroupCode() != 0) {
            List<CouponUser> mine = list(Wrappers.<CouponUser>lambdaQuery()
                    .eq(CouponUser::getUserId, userId));
            for (CouponUser cu : mine) {
                Coupon c = couponService.getById(cu.getCouponId());
                if (c != null && coupon.getMutexGroupCode().equals(c.getMutexGroupCode())
                        && c.getMutexGroupCode() != 0) {
                    throw new RuntimeException("与已持有券互斥，不可同时领取");
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validStart = now;
        LocalDateTime validEnd;
        if (coupon.getValidMode() == CouponValidModeEnum.FIXED_TIME) {
            validStart = coupon.getValidStart();
            validEnd = coupon.getValidEnd();
        } else {
            int days = coupon.getReceiveValidDays() == null ? 0 : coupon.getReceiveValidDays();
            validEnd = now.plusDays(days);
        }
        CouponUser userCoupon = new CouponUser();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setValidStart(validStart);
        userCoupon.setValidEnd(validEnd);
        userCoupon.setUseStatus(CouponUseStatusEnum.UNUSED);
        userCoupon.setCreateTime(now);
        userCoupon.setUpdateTime(now);
        save(userCoupon);

        coupon.setReceiveQuota((coupon.getReceiveQuota() == null ? 0 : coupon.getReceiveQuota()) + 1);
        coupon.setUpdateTime(now);
        couponService.updateById(coupon);
    }

    @Override
    public IPage<CouponUser> myCoupons(Long userId, Integer useStatus, Integer page, Integer size) {
        Page<CouponUser> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        return page(p, Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(useStatus != null, CouponUser::getUseStatus, useStatus)
                .orderByDesc(CouponUser::getCreateTime));
    }

    @Override
    public List<CouponUser> listAvailable(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return list(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getUseStatus, CouponUseStatusEnum.UNUSED)
                .le(CouponUser::getValidStart, now)
                .ge(CouponUser::getValidEnd, now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal useCoupon(Long userId, CouponUseDTO dto) {
        CouponUser userCoupon = getById(dto.getUserCouponId());
        if (userCoupon == null) {
            throw new RuntimeException("用户券不存在");
        }
        if (!userCoupon.getUserId().equals(userId)) {
            throw new RuntimeException("无权使用该券");
        }
        if (userCoupon.getUseStatus() != CouponUseStatusEnum.UNUSED) {
            throw new RuntimeException("该券不可使用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (userCoupon.getValidStart() != null && now.isBefore(userCoupon.getValidStart())) {
            throw new RuntimeException("该券未到生效时间");
        }
        if (userCoupon.getValidEnd() != null && now.isAfter(userCoupon.getValidEnd())) {
            throw new RuntimeException("该券已过期");
        }
        Coupon coupon = couponService.getById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new RuntimeException("优惠券模板不存在");
        }
        BigDecimal discount = calcDiscount(coupon, dto.getOrderItemId());
        userCoupon.setUseStatus(CouponUseStatusEnum.USED);
        userCoupon.setUpdateTime(now);
        updateById(userCoupon);
        CouponOrderRel rel = new CouponOrderRel();
        rel.setOrderId(dto.getOrderId());
        rel.setOrderItemId(dto.getOrderItemId() == null ? 0L : dto.getOrderItemId());
        rel.setUserCouponId(userCoupon.getId());
        rel.setDiscountAmount(discount);
        rel.setRelStatus(1);
        rel.setUseTime(now);
        rel.setCreateTime(now);
        couponOrderRelMapper.insert(rel);
        coupon.setUsedQuota((coupon.getUsedQuota() == null ? 0 : coupon.getUsedQuota()) + 1);
        coupon.setUpdateTime(now);
        couponService.updateById(coupon);
        return discount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundRollback(Long userCouponId) {
        CouponUser userCoupon = getById(userCouponId);
        if (userCoupon == null) {
            return;
        }
        userCoupon.setUseStatus(CouponUseStatusEnum.RETURNED);
        userCoupon.setUpdateTime(LocalDateTime.now());
        updateById(userCoupon);

        Coupon coupon = couponService.getById(userCoupon.getCouponId());
        if (coupon != null) {
            int used = coupon.getUsedQuota() == null ? 0 : coupon.getUsedQuota();
            coupon.setUsedQuota(Math.max(0, used - 1));
            coupon.setUpdateTime(LocalDateTime.now());
            couponService.updateById(coupon);
        }

        List<CouponOrderRel> rels = couponOrderRelMapper.selectList(Wrappers.<CouponOrderRel>lambdaQuery()
                .eq(CouponOrderRel::getUserCouponId, userCouponId)
                .eq(CouponOrderRel::getRelStatus, 1));
        for (CouponOrderRel rel : rels) {
            rel.setRelStatus(2);
            rel.setRefundTime(LocalDateTime.now());
            couponOrderRelMapper.updateById(rel);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<CouponUser> expired = list(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUseStatus, CouponUseStatusEnum.UNUSED)
                .lt(CouponUser::getValidEnd, now));
        for (CouponUser cu : expired) {
            cu.setUseStatus(CouponUseStatusEnum.EXPIRED);
            cu.setUpdateTime(now);
        }
        if (!expired.isEmpty()) {
            updateBatchById(expired);
            log.info("优惠券过期扫描：处理 {} 张", expired.size());
        }
    }

    private BigDecimal calcDiscount(Coupon coupon, Long orderItemId) {
        CouponTypeEnum type = coupon.getCouponType();
        if (type == CouponTypeEnum.NO_THRESHOLD) {
            return coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
        }
        if (type == CouponTypeEnum.FULL_REDUCTION) {
            return coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
        }
        if (type == CouponTypeEnum.DISCOUNT) {
            BigDecimal rate = coupon.getDiscountRate() == null ? BigDecimal.ONE : coupon.getDiscountRate();
            BigDecimal divisor = BigDecimal.valueOf(10);
            BigDecimal percent = rate.divide(divisor, 4, java.math.RoundingMode.HALF_UP);
            BigDecimal maxDiscount = coupon.getMaxDiscount() == null ? BigDecimal.ZERO : coupon.getMaxDiscount();

            if (orderItemId != null && orderItemId > 0) {
                return maxDiscount;
            }
            return maxDiscount;
        }

        return coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
    }

}
