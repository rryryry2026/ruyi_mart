package com.ruyi.ruyi_mart.module.stock.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.log.annotation.OpLog;
import com.ruyi.ruyi_mart.module.stock.service.StockService;
import com.ruyi.ruyi_mart.module.stock.vo.StockInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

//用户端商品库存接口
@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/init")
    @OpLog(module = "库存管理", action = "设置库存")
    public Result<Void> init(@RequestParam Long productId,@RequestParam Integer total){
        stockService.initStock(productId,total);
        return Result.success();
    }

    /**
     * 商品"还剩几件"。
     * 只回 available：这个接口是游客可见的（/stock/** 在 SecurityConfig 里 permitAll），
     * 不能把 total（进货量）、locked（有多少人正下单没付款）一起吐出去。
     * 库存明细只走管理端接口。
     */
    @GetMapping("/info")
    public Result<StockInfoVO> info(@RequestParam Long productId){
        return Result.success(StockInfoVO.from(stockService.getByProductId(productId)));
    }
}
