package com.ruyi.ruyi_mart.module.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**商品分类业务的mapper层。*/
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
