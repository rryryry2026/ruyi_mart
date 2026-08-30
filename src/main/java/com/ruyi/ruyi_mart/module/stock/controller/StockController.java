package com.ruyi.ruyi_mart.module.stock.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
