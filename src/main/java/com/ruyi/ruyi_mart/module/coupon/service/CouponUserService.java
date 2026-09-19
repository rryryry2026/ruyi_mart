package com.ruyi.ruyi_mart.module.coupon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponReceiveDTO;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponUseDTO;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponUser;
import com.ruyi.ruyi_mart.module.coupon.vo.CouponTemplateVO;
import com.ruyi.ruyi_mart.module.coupon.vo.CouponUserVO;

import java.math.BigDecimal;
import java.util.List;

//用户端优惠券业务接口，声明方法。
public interface CouponUserService extends IService<CouponUser> {

    /**用户领券（userId 从登录态取，dto 只带 couponId）*/
    void receiveCoupon(Long userId, CouponReceiveDTO dto);

    /**我的券包（分页，按 useStatus 可选筛选）。带券模板的面额/门槛等展示字段*/
    IPage<CouponUserVO> myCoupons(Long userId, Integer useStatus, Integer page, Integer size);

    /**
     * 结算可用券列表（未用且在有效期内）。
     * 传了 orderAmount 时会按订单金额算出每张券的抵扣额，
     * 并把不满足门槛（抵扣为 0）的券滤掉，方便结算页直接展示可用优惠。
     */
    List<CouponUserVO> listAvailable(Long userId, BigDecimal orderAmount);

    /**领券中心：当前用户还能领取的券模板（已按领取上限、库存、互斥组预筛过）*/
    List<CouponTemplateVO> listReceivable(Long userId);

    /**核销（结算用券，返回本次抵扣金额）*/
    BigDecimal useCoupon(Long userId, CouponUseDTO dto);

    /**退款回滚（订单退款时调用，恢复券与额度）*/
    void refundRollback(Long userCouponId);

    /**过期扫描（@Scheduled 定时任务调用，把过期未用券置为 EXPIRED）*/
    void scanExpired();
}
