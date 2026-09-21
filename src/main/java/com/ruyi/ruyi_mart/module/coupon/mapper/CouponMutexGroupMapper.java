package com.ruyi.ruyi_mart.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponMutexGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CouponMutexGroupMapper extends BaseMapper<CouponMutexGroup> {

    /**
     * 按互斥组编码加行锁（领互斥券时用）。
     *
     * 互斥是"跨券"的规则：领取时只锁住待领的那张券模板行是不够的 ——
     * 同一用户并发领两张互斥券时，两个事务各锁各的券行、各自的互斥检查
     * 都看不见对方还没提交的那条领取记录，结果两张互斥券都领到了。
     * 所以额外把互斥组那一行锁住，让同组的领券请求排队。
     */
    @Select("SELECT id FROM coupon_mutex_group WHERE group_code = #{groupCode} FOR UPDATE")
    Long lockByGroupCode(@Param("groupCode") Long groupCode);
}
