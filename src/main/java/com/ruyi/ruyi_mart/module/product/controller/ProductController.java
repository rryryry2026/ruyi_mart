package com.ruyi.ruyi_mart.module.product.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.product.entity.Product;
import com.ruyi.ruyi_mart.module.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
    public Result<Long> add(@RequestBody Product product){
        productService.save(product);
        return Result.success(product.getId());
    }

    @PutMapping
    public Result<Void> update(@RequestBody Product product){
        productService.updateById(product);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id){
        productService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id){
        return Result.success(productService.getById(id));
    }

    @GetMapping("/list")
    public Result<?> list(){
        return Result.success(productService.list());
    }

}
