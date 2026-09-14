package com.ruyi.ruyi_mart.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
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
import com.ruyi.ruyi_mart.module.coupon.vo.CouponTemplateVO;
import com.ruyi.ruyi_mart.module.coupon.vo.CouponUserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            throw new BusinessException(ResultCode.FAIL, "优惠券不存在");
        }
        if(coupon.getStatus() == null || coupon.getStatus() != 1){
            throw new BusinessException(ResultCode.FAIL, "优惠券不在发放中");
        }
        if(coupon.getTotalQuota() != null && coupon.getTotalQuota() > 0){
            if(coupon.getReceiveQuota() >= coupon.getTotalQuota()){
                throw new BusinessException(ResultCode.FAIL, "优惠券已领完");
            }
        }

        long owned = count(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getCouponId, coupon.getId()));
        int limit = coupon.getLimitPerPerson() == null ? 1 : coupon.getLimitPerPerson();
        if (owned >= limit) {
            throw new BusinessException(ResultCode.FAIL, "已达到单人领取上限");
        }

        if (coupon.getMutexGroupCode() != null && coupon.getMutexGroupCode() != 0) {
            List<CouponUser> mine = list(Wrappers.<CouponUser>lambdaQuery()
                    .eq(CouponUser::getUserId, userId));
            for (CouponUser cu : mine) {
                Coupon c = couponService.getById(cu.getCouponId());
                if (c != null && coupon.getMutexGroupCode().equals(c.getMutexGroupCode())
                        && c.getMutexGroupCode() != 0) {
                    throw new BusinessException(ResultCode.FAIL, "与已持有券互斥，不可同时领取");
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
    public IPage<CouponUserVO> myCoupons(Long userId, Integer useStatus, Integer page, Integer size) {
        Page<CouponUser> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        IPage<CouponUser> entityPage = page(p, Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(useStatus != null, CouponUser::getUseStatus, useStatus)
                .orderByDesc(CouponUser::getCreateTime));

        // 批量取券模板，避免逐条查（N+1）
        Map<Long, Coupon> couponMap = loadCouponMap(entityPage.getRecords());
        List<CouponUserVO> voList = entityPage.getRecords().stream()
                .map(cu -> toUserVO(cu, couponMap.get(cu.getCouponId()), null))
                .collect(Collectors.toList());

        Page<CouponUserVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<CouponUserVO> listAvailable(Long userId, BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponUser> mine = list(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getUseStatus, CouponUseStatusEnum.UNUSED)
                .le(CouponUser::getValidStart, now)
                .ge(CouponUser::getValidEnd, now));

        Map<Long, Coupon> couponMap = loadCouponMap(mine);
        List<CouponUserVO> result = new ArrayList<>();
        for (CouponUser cu : mine) {
            Coupon coupon = couponMap.get(cu.getCouponId());
            BigDecimal discount = null;
            if (orderAmount != null && coupon != null) {
                discount = calcDiscount(coupon, orderAmount);
                // 传了订单金额，就把不满足门槛（抵扣为 0）的券滤掉，
                // 免得结算页列出一堆用不了的券
                if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
            }
            result.add(toUserVO(cu, coupon, discount));
        }
        return result;
    }

    @Override
    public List<CouponTemplateVO> listReceivable(Long userId) {
        // 发放中、且未被隐藏的券模板
        List<Coupon> templates = couponService.list(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getStatus, 1)
                .eq(Coupon::getIsElimination, 0)
                .orderByDesc(Coupon::getCreateTime));
        if (templates.isEmpty()) {
            return new ArrayList<>();
        }

        // 当前用户已持有的券，用于算"已领张数"与互斥组
        List<CouponUser> mine = list(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId));
        Map<Long, Long> ownedCountMap = mine.stream()
                .collect(Collectors.groupingBy(CouponUser::getCouponId, Collectors.counting()));

        // 已持有券所属的互斥组
        Set<Long> heldMutexGroups = new HashSet<>();
        if (!mine.isEmpty()) {
            Set<Long> heldCouponIds = mine.stream().map(CouponUser::getCouponId).collect(Collectors.toSet());
            for (Coupon c : couponService.listByIds(heldCouponIds)) {
                if (c.getMutexGroupCode() != null && c.getMutexGroupCode() != 0) {
                    heldMutexGroups.add(c.getMutexGroupCode());
                }
            }
        }

        List<CouponTemplateVO> result = new ArrayList<>();
        for (Coupon coupon : templates) {
            int total = coupon.getTotalQuota() == null ? 0 : coupon.getTotalQuota();
            int received = coupon.getReceiveQuota() == null ? 0 : coupon.getReceiveQuota();
            Integer remainQuota = total > 0 ? Math.max(0, total - received) : null;
            if (remainQuota != null && remainQuota <= 0) {
                continue; // 已领完
            }

            long owned = ownedCountMap.getOrDefault(coupon.getId(), 0L);
            int limit = coupon.getLimitPerPerson() == null ? 1 : coupon.getLimitPerPerson();

            // 与 receiveCoupon 的校验保持一致：互斥组里只要已持有任意一张，就不能再领
            boolean blockedByMutex = coupon.getMutexGroupCode() != null
                    && coupon.getMutexGroupCode() != 0
                    && heldMutexGroups.contains(coupon.getMutexGroupCode());

            int remainToReceive = blockedByMutex ? 0 : (int) Math.max(0, limit - owned);
            if (remainToReceive <= 0) {
                continue; // 已达单人上限 / 被互斥挡住
            }

            CouponTemplateVO vo = new CouponTemplateVO();
            vo.setId(coupon.getId());
            vo.setCouponNo(coupon.getCouponNo());
            vo.setActivityName(coupon.getActivityName());
            vo.setCouponType(coupon.getCouponType());
            vo.setFaceValue(coupon.getFaceValue());
            vo.setDiscountRate(coupon.getDiscountRate());
            vo.setMaxDiscount(coupon.getMaxDiscount());
            vo.setMinSpend(coupon.getMinSpend());
            vo.setValidMode(coupon.getValidMode());
            vo.setValidStart(coupon.getValidStart());
            vo.setValidEnd(coupon.getValidEnd());
            vo.setReceiveValidDays(coupon.getReceiveValidDays());
            vo.setLimitPerPerson(limit);
            vo.setReceiveQuota(received);
            vo.setTotalQuota(total);
            vo.setUseScope(coupon.getUseScope());
            vo.setOwnedCount((int) owned);
            vo.setRemainToReceive(remainToReceive);
            vo.setRemainQuota(remainQuota);
            result.add(vo);
        }
        return result;
    }

    /** 批量取券模板，避免逐条查询 */
    private Map<Long, Coupon> loadCouponMap(List<CouponUser> userCoupons) {
        if (userCoupons == null || userCoupons.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = userCoupons.stream()
                .map(CouponUser::getCouponId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return couponService.listByIds(ids).stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c, (a, b) -> a));
    }

    private CouponUserVO toUserVO(CouponUser cu, Coupon coupon, BigDecimal discountAmount) {
        CouponUserVO vo = new CouponUserVO();
        vo.setId(cu.getId());
        vo.setCouponId(cu.getCouponId());
        vo.setUseStatus(cu.getUseStatus());
        vo.setValidStart(cu.getValidStart());
        vo.setValidEnd(cu.getValidEnd());
        vo.setCreateTime(cu.getCreateTime());
        vo.setDiscountAmount(discountAmount);
        if (coupon != null) {
            vo.setActivityName(coupon.getActivityName());
            vo.setCouponType(coupon.getCouponType());
            vo.setFaceValue(coupon.getFaceValue());
            vo.setDiscountRate(coupon.getDiscountRate());
            vo.setMaxDiscount(coupon.getMaxDiscount());
            vo.setMinSpend(coupon.getMinSpend());
            vo.setUseScope(coupon.getUseScope());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal useCoupon(Long userId, CouponUseDTO dto) {
        CouponUser userCoupon = getById(dto.getUserCouponId());
        if (userCoupon == null) {
            throw new BusinessException(ResultCode.FAIL, "用户券不存在");
        }
        if (!userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FAIL, "无权使用该券");
        }
        if (userCoupon.getUseStatus() != CouponUseStatusEnum.UNUSED) {
            throw new BusinessException(ResultCode.FAIL, "该券不可使用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (userCoupon.getValidStart() != null && now.isBefore(userCoupon.getValidStart())) {
            throw new BusinessException(ResultCode.FAIL, "该券未到生效时间");
        }
        if (userCoupon.getValidEnd() != null && now.isAfter(userCoupon.getValidEnd())) {
            throw new BusinessException(ResultCode.FAIL, "该券已过期");
        }
        Coupon coupon = couponService.getById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new BusinessException(ResultCode.FAIL, "优惠券模板不存在");
        }
        BigDecimal discount = calcDiscount(coupon, dto.getOrderAmount());
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.FAIL, "该优惠券不满足使用条件或抵扣金额为0");
        }

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

    private BigDecimal calcDiscount(Coupon coupon, BigDecimal orderAmount) {
        CouponTypeEnum type = coupon.getCouponType();

        if (orderAmount == null) {
            if (type == CouponTypeEnum.DISCOUNT) {
                return coupon.getMaxDiscount() == null ? BigDecimal.ZERO : coupon.getMaxDiscount();
            }
            return coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
        }

        if (type == CouponTypeEnum.NO_THRESHOLD) {
            return coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
        }

        if (type == CouponTypeEnum.FULL_REDUCTION) {
            BigDecimal minSpend = coupon.getMinSpend() == null ? BigDecimal.ZERO : coupon.getMinSpend();
            if (orderAmount.compareTo(minSpend) < 0) {
                return BigDecimal.ZERO;
            }
            return coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
        }

        if (type == CouponTypeEnum.DISCOUNT) {
            BigDecimal rate = coupon.getDiscountRate() == null ? BigDecimal.TEN : coupon.getDiscountRate();
            BigDecimal percent = rate.divide(BigDecimal.TEN, 4, java.math.RoundingMode.HALF_UP);
            BigDecimal discount = orderAmount.multiply(BigDecimal.ONE.subtract(percent));
            discount = discount.setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal maxDiscount = coupon.getMaxDiscount() == null ? BigDecimal.ZERO : coupon.getMaxDiscount();
            if (maxDiscount.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(maxDiscount) > 0) {
                discount = maxDiscount;
            }
            if (discount.compareTo(orderAmount) > 0) {
                discount = orderAmount;
            }
            return discount;
        }

        return coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
    }


}
