package com.ruyi.ruyi_mart.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponMutexGroup;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 互斥组 Mapper。
 *
 * 互斥组是一份"配置数据"：建/改券时按需自动建档（见 CouponServiceImpl）。
 * 注意核销时并不读这张表——"一笔订单只能用一张券"是比"同组不能叠加"更强的约束，
 * 已经把叠加问题整个覆盖掉；配置保留下来是为了将来放宽成"同组多券互斥"时不用重建。
 */
@Mapper
public interface CouponMutexGroupMapper extends BaseMapper<CouponMutexGroup> {

    /**
     * 互斥组不存在时补一条。
     * 用 INSERT IGNORE：group_code 上有唯一索引，并发建券时后到的那条被忽略，不会报错。
     */
    @Insert("INSERT IGNORE INTO coupon_mutex_group (group_code, group_name, create_time) " +
            "VALUES (#{groupCode}, #{groupName}, NOW())")
    int insertIfAbsent(@Param("groupCode") Long groupCode, @Param("groupName") String groupName);
}
