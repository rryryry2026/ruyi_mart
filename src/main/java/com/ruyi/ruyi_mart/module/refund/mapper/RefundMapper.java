package com.ruyi.ruyi_mart.module.refund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.refund.entity.Refund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RefundMapper extends BaseMapper<Refund> {

    /**
     * 只有当前状态等于 from 才改成 to，返回影响行数（0 表示已被别人审过）。
     *
     * 审核退款必须先用它抢状态，再去退库存/回滚优惠券：
     * 原先的写法是"查出来看状态 + 最后 updateById"，两个管理员同时点"同意"
     * 会双双通过状态检查，于是库存回补两次、订单退款两次。
     * 与 OrderMapper/CouponUserMapper 的 changeStatusIf 同一套做法。
     */
    @Update("UPDATE refund_info SET status = #{to}, update_time = NOW() " +
            "WHERE id = #{id} AND status = #{from}")
    int changeStatusIf(@Param("id") Long id,
                       @Param("from") Integer from,
                       @Param("to") Integer to);
}
