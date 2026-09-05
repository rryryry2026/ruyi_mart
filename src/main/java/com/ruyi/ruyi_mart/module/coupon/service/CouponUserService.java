package com.ruyi.ruyi_mart.module.coupon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponReceiveDTO;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponUseDTO;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponUser;

import java.util.List;

public interface CouponUserService extends IService<CouponUser> {

    /**用户领券（userId 从登录态取，dto 只带 couponId）*/
    void receiveCoupon(Long userId, CouponReceiveDTO dto);

    /**我的券包（分页，按 useStatus 可选筛选）*/
    IPage<CouponUser> myCoupons(Long userId, Integer useStatus, Integer page, Integer size);

    /**结算可用券列表（未用且在有效期内）*/
    List<CouponUser> listAvailable(Long userId);

    /**核销（结算用券，返回本次抵扣金额）*/
    java.math.BigDecimal useCoupon(Long userId, CouponUseDTO dto);

    /**退款回滚（订单退款时调用，恢复券与额度）*/
    void refundRollback(Long userCouponId);

    /**过期扫描（@Scheduled 定时任务调用，把过期未用券置为 EXPIRED）*/
    void scanExpired();
}
