package com.ruyi.ruyi_mart.module.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.stock.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    /**预扣*/
    @Update("UPDATE stock SET available = available - #{n}, locked = locked + #{n} " +
            "WHERE product_id = #{id} AND available >= #{n}")
    int preDeduct(@Param("id") Long id,@Param("n") Integer n);

    /**确认扣减*/
    @Update("UPDATE stock SET locked = locked - #{n} " +
            "WHERE product_id = #{id} AND locked >= #{n}")
    int confirmDeduct(@Param("id") Long id, @Param("n") Integer n);

    /**回补*/
    @Update("UPDATE stock SET available = available + #{n}, locked = locked - #{n} " +
            "WHERE product_id = #{id} AND locked >= #{n}")
    int rollback(@Param("id") Long id, @Param("n") Integer n);

    /**退款回补*/
    @Update("UPDATE stock SET available = available + #{n}, version = version + 1 " +
            "WHERE product_id = #{id} AND available + #{n} <= total")
    int refundBack(@Param("id") Long id, @Param("n") Integer n);

    /**
     * 重设库存总量（可用库存按差额同步调整）
     * 单条原子 SQL 完成"读-改-写"，避免先查后改带来的并发竞态。
     * 注意赋值顺序：MySQL 的 SET 是从左到右求值，后面的表达式读到的是前面刚赋的新值，
     * 因此 available 必须排在 total 之前——否则表达式里的 total 已被覆盖，差额恒为 0。
     */
    @Update("UPDATE stock SET available = available + (#{total} - total), total = #{total}, " +
            "version = version + 1, update_time = NOW() WHERE product_id = #{id}")
    int resetTotal(@Param("id") Long id, @Param("total") Integer total);
}
