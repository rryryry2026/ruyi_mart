package com.ruyi.ruyi_mart.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.coupon.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 券模板 Mapper。
 *
 * 计数器（领取额度、核销数）一律用单条原子 SQL 完成"读-改-写"，
 * 与 StockMapper 的库存扣减同一套做法：
 * 先把判断条件写进 WHERE，再靠影响行数判断是否成功，
 * 避免"先查后写"在并发下互相覆盖。
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 取券模板并加行锁（领券时调用）。
     *
     * 领券要同时看"总发行量剩多少"和"这个人已经领了几张"，而后者是
     * "先查张数、再插一条"，光靠下面的原子计数拦不住：
     * 同一个用户同时点 8 次，8 个事务都查到"已领 0 张"，
     * limit_per_person=1 也会插出 8 条记录（实测如此）。
     * 所以在事务开始时先锁住模板这一行，把同一张券的领券请求串起来。
     * 锁的粒度只到"同一张券"，不同券之间互不影响。
     */
    @Select("SELECT * FROM coupon WHERE id = #{id} FOR UPDATE")
    Coupon selectByIdForUpdate(@Param("id") Long id);

    /**
     * 占用一份领取额度（领券时调用）。
     * total_quota = 0 表示不限量，此时只自增、不加条件。
     * 返回 0 表示已领完（或被并发抢走最后一份）。
     */
    @Update("UPDATE coupon SET receive_quota = receive_quota + 1, update_time = NOW() " +
            "WHERE id = #{id} AND (total_quota = 0 OR receive_quota < total_quota)")
    int occupyReceiveQuota(@Param("id") Long id);

    /** 核销计数自增（用券时调用，与库存的 confirm 同理，只动自己这一列） */
    @Update("UPDATE coupon SET used_quota = used_quota + 1, update_time = NOW() " +
            "WHERE id = #{id}")
    int increaseUsedQuota(@Param("id") Long id);

    /** 核销计数回滚（退款回滚时调用）；带 used_quota > 0 兜底，不会减成负数 */
    @Update("UPDATE coupon SET used_quota = used_quota - 1, update_time = NOW() " +
            "WHERE id = #{id} AND used_quota > 0")
    int decreaseUsedQuota(@Param("id") Long id);
}
