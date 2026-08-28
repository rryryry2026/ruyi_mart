package com.ruyi.ruyi_mart.module.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.product.entity.Product;

public interface ProductService extends IService<Product> {

    int deductStock(Long id, Integer quantity);
    int addStock(Long id, Integer quantity);
}
