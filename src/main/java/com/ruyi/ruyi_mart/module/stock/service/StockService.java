package com.ruyi.ruyi_mart.module.stock.service;

import com.ruyi.ruyi_mart.module.stock.entity.Stock;

public interface StockService {

    //初始化/补货商品库存
    void initStock(Long productId, Integer total);

    /**预扣库存*/
    boolean tryLock(Long productId,Integer count);

    /**确认扣减*/
    void confirm(Long productId,Integer count);

    /**回补*/
    void release(Long productId,Integer count);

    /**查询商品库存明细（total/available/locked）*/
    Stock getByProductId(Long productId);
}
