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
}
