package com.ruyi.ruyi_mart.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponMutexGroup;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 互斥组 Mapper。
 *
 * 互斥组是一份"配置数据"：券上存 mutex_group_code，核销时靠它判断"同组券不能叠加使用"。
 * 目前没有独立的管理端页面，所以建/改券时按需自动建档（见 CouponServiceImpl）。
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
