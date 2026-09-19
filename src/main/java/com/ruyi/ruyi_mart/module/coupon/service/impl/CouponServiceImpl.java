package com.ruyi.ruyi_mart.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponCreateDTO;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponQueryDTO;
import com.ruyi.ruyi_mart.module.coupon.entity.Coupon;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponUser;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponTypeEnum;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponValidModeEnum;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponMapper;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponUserMapper;
import com.ruyi.ruyi_mart.module.coupon.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

//优惠券模板的管理端实现。
@Service
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    /** 模板状态：1 发放中 */
    private static final int STATUS_RELEASING = 1;

    @Autowired
    private CouponUserMapper couponUserMapper;

    /**管理端分页列表*/
    @Override
    public IPage<Coupon> listCoupons(CouponQueryDTO dto){
        // 前端可能传空串（转成 null），这里兜底，避免拆箱 NPE
        int pageNum = dto.getPage() == null ? 1 : dto.getPage();
        int pageSize = dto.getSize() == null ? 10 : dto.getSize();
        Page<Coupon> page = new Page<>(pageNum, pageSize);
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
        applyDto(coupon, dto);
        coupon.setCouponNo(generateCouponNo());
        coupon.setReceiveQuota(0);
        coupon.setUsedQuota(0);
        coupon.setReleaseTime(LocalDateTime.now());
        // 新建的券默认"发放中"（与 Banner 新建默认启用一致）；
        // 注意不能依赖数据库默认值——coupon.status 的默认是 0（未开始），
        // 那样建出来的券在领券中心不显示、也领不到
        coupon.setStatus(dto.getStatus() == null ? STATUS_RELEASING : dto.getStatus());
        coupon.setIsElimination(0);
        coupon.setCreateTime(LocalDateTime.now());
        coupon.setUpdateTime(LocalDateTime.now());
        validateCouponRule(coupon);
        save(coupon);
    }

    /**管理端更新券*/
    @Override
    public void updateCoupon(Long id, CouponCreateDTO dto){
        Coupon coupon = getById(id);
        if(coupon == null){
            throw new BusinessException(ResultCode.NOT_FIND, "优惠券不存在: " + id);
        }
        applyDto(coupon, dto);
        if (dto.getStatus() != null) {
            coupon.setStatus(dto.getStatus());
        }
        coupon.setUpdateTime(LocalDateTime.now());
        // 用合并后的完整状态校验，保证"改出来的券"和"新建的券"受同一套规则约束
        validateCouponRule(coupon);
        updateById(coupon);
    }

    /**管理端删除券（物理删除）*/
    @Override
    public void deleteCoupon(Long id) {
        Coupon coupon = getById(id);
        if(coupon == null){
            throw new BusinessException(ResultCode.NOT_FIND, "优惠券不存在: " + id);
        }
        // 已经有人领过的模板不允许物理删除：删掉会让这些券变成孤儿，
        // 用户券包里只剩一个查不到的 couponId（面额、门槛全空白）。
        // 想让券停止发放，应该用"改状态为已结束/作废"。
        long held = couponUserMapper.selectCount(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getCouponId, id));
        if (held > 0) {
            throw new BusinessException(ResultCode.FAIL,
                    "该券已被 " + held + " 位用户领取，不能删除；请改为把状态置为已结束或作废");
        }
        removeById(id);
    }

    /**管理端启停 / 改模板状态*/
    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null) {
            throw new BusinessException(ResultCode.FAIL, "状态不能为空");
        }
        boolean updated = lambdaUpdate()
                .eq(Coupon::getId, id)
                .set(Coupon::getStatus, status)
                .set(Coupon::getUpdateTime, LocalDateTime.now())
                .update();
        if (!updated) {
            throw new BusinessException(ResultCode.NOT_FIND, "优惠券不存在: " + id);
        }
    }

    /** 把 DTO 的字段搬到实体上（建/改共用，避免两处漏字段） */
    private void applyDto(Coupon coupon, CouponCreateDTO dto){
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
    }

    /**
     * 券模板的业务规则校验。
     * 这些是"按类型/模式组合必填"的规则，注解表达不了，只能手写；
     * 建券和改券都走这里，避免只堵住一个入口。
     */
    private void validateCouponRule(Coupon coupon){
        CouponTypeEnum type = coupon.getCouponType();
        if (type == null) {
            throw new BusinessException(ResultCode.FAIL, "券类型不能为空");
        }

        // 面额 / 折扣率：按类型二选一必填
        if (type == CouponTypeEnum.DISCOUNT) {
            BigDecimal rate = coupon.getDiscountRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.TEN) >= 0) {
                throw new BusinessException(ResultCode.FAIL, "折扣券的折扣率必须在 0~10 之间（如 8.8 表示 88 折）");
            }
        } else {
            BigDecimal face = coupon.getFaceValue();
            if (face == null || face.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ResultCode.FAIL, "面额必须大于 0");
            }
            // 满减券没有门槛就等于无门槛券，属于配置错误
            if (type == CouponTypeEnum.FULL_REDUCTION) {
                BigDecimal minSpend = coupon.getMinSpend();
                if (minSpend == null || minSpend.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(ResultCode.FAIL, "满减券必须设置大于 0 的使用门槛");
                }
            }
        }

        // 有效期：两种模式各自的必填项
        CouponValidModeEnum mode = coupon.getValidMode();
        if (mode == null) {
            throw new BusinessException(ResultCode.FAIL, "有效期模式不能为空");
        }
        if (mode == CouponValidModeEnum.FIXED_TIME) {
            if (coupon.getValidStart() == null || coupon.getValidEnd() == null) {
                // 少了这两个时间，领取人的券会 "永不过期"（核销时的判空分支会直接放行）
                throw new BusinessException(ResultCode.FAIL, "固定时间模式必须同时填写起止时间");
            }
            if (!coupon.getValidStart().isBefore(coupon.getValidEnd())) {
                throw new BusinessException(ResultCode.FAIL, "有效期的开始时间必须早于结束时间");
            }
        } else {
            Integer days = coupon.getReceiveValidDays();
            if (days == null || days <= 0) {
                throw new BusinessException(ResultCode.FAIL, "领券后生效模式必须填写大于 0 的有效天数");
            }
        }

        if (coupon.getTotalQuota() != null && coupon.getTotalQuota() < 0) {
            throw new BusinessException(ResultCode.FAIL, "发行总量不能为负数");
        }
        if (coupon.getLimitPerPerson() != null && coupon.getLimitPerPerson() < 1) {
            throw new BusinessException(ResultCode.FAIL, "单人限领至少为 1 张");
        }
    }

    /** 券编码：与订单号/退款单号同风格 */
    private String generateCouponNo(){
        return "C" + System.currentTimeMillis()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 4);
    }
}
