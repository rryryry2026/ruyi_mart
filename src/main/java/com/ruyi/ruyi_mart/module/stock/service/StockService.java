package com.ruyi.ruyi_mart.module.stock.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.module.stock.entity.Stock;
import com.ruyi.ruyi_mart.module.stock.vo.ProductStockVO;

/**商品库存接口，声明方法。*/
public interface StockService {

    /**初始化/补货商品库存*/
    void initStock(Long productId, Integer total);

    /**预扣库存*/
    boolean tryLock(Long productId,Integer count);

    /**确认扣减*/
    void confirm(Long productId,Integer count);

    /**回补*/
    void release(Long productId,Integer count);

    /**退款回补*/
    void refund(Long productId,Integer count);

    /**
     * 查询商品库存明细（total/available/locked）。
     * 供内部与管理端使用；用户端接口只暴露 available，不要把这个实体直接返回给游客。
     */
    Stock getByProductId(Long productId);

    /**管理端：商品维度库存分页（无库存记录的商品也返回，便于初始化）*/
    Page<ProductStockVO> adminStockPage(String keyword, Long categoryId, int pageNum, int pageSize);
}
