package com.ruyi.ruyi_mart.module.stock.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.stock.service.StockService;
import com.ruyi.ruyi_mart.module.stock.vo.ProductStockVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端库存接口：商品维度的库存列表（现有 /stock/info 只能按单个商品查询）
 */
@RestController
@RequestMapping("/admin/stock")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStockController {

    @Autowired
    private StockService stockService;

    /**库存分页（商品名模糊 / 分类筛选；无库存记录的商品也返回，便于初始化）*/
    @GetMapping("/page")
    public Result<Page<ProductStockVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long categoryId) {
        return Result.success(stockService.adminStockPage(keyword, categoryId, pageNum, pageSize));
    }
}
