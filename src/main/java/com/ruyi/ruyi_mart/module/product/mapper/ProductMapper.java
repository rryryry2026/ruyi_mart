package com.ruyi.ruyi_mart.module.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("UPDATE product SET stock = stock - #{quantity} Where id = #{id} AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id,@Param("quantity") Integer quantity);
}
