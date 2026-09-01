package com.ruyi.ruyi_mart.module.stock.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.stock.entity.Stock;
import com.ruyi.ruyi_mart.module.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/init")
    public Result<Void> init(@RequestParam Long productId,@RequestParam Integer total){
        stockService.initStock(productId,total);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<Stock> info(@RequestParam Long productId){
        return Result.success(stockService.getByProductId(productId));
    }
}
