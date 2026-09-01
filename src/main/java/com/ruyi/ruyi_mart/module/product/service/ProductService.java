package com.ruyi.ruyi_mart.module.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.product.dto.ProductQueryDTO;
import com.ruyi.ruyi_mart.module.product.entity.Product;

public interface ProductService extends IService<Product> {
    /** 商品分页查询：支持名称搜索、分类筛选、上下架状态筛选 */
    Page<Product> pageQuery(ProductQueryDTO query);

}
