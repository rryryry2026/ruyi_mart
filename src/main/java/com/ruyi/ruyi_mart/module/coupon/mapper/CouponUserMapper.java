package com.ruyi.ruyi_mart.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户券 Mapper。
 *
 * 状态流转一律用"条件更新 + 影响行数判定"，把原状态写进 WHERE，
 * 这样并发下只有第一个请求能改成功，避免同一张券被核销两次或被重复回滚。
 * 与 StockMapper 靠 WHERE 条件做 CAS 是同一个思路。
 */
@Mapper
public interface CouponUserMapper extends BaseMapper<CouponUser> {

    /**
     * 只有当前状态等于 from 才改成 to。
     * 返回 0 表示状态已被别人改过（例如已核销 / 已回滚），调用方应当拒绝。
     *
     * @param from 期望的原状态码（CouponUseStatusEnum 的 code）
     * @param to   目标状态码
     */
    @Update("UPDATE coupon_user SET use_status = #{to}, update_time = NOW() " +
            "WHERE id = #{id} AND use_status = #{from}")
    int changeStatusIf(@Param("id") Long id,
                       @Param("from") Integer from,
                       @Param("to") Integer to);
}
