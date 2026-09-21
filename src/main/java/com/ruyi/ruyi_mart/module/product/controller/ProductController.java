package com.ruyi.ruyi_mart.module.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.product.dto.ProductQueryDTO;
import com.ruyi.ruyi_mart.module.product.entity.Product;
import com.ruyi.ruyi_mart.module.product.service.ProductService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**商品业务模块的接口层。*/
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**添加商品。*/
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> add(@RequestBody Product product){
        productService.save(product);
        return Result.success(product.getId());
    }

    /**修改商品。*/
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@RequestBody Product product){
        productService.updateById(product);
        return Result.success();
    }

    /**删除商品。*/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id){
        productService.removeById(id);
        return Result.success();
    }

    /**获取单个商品。*/
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id){
        return Result.success(productService.getById(id));
    }

    /**获取商品列表。*/
    @GetMapping("/list")
    public Result<?> list(){
        return Result.success(productService.list());
    }

    /**商品分页查询。*/
    @GetMapping("/page")
    public Result<Page<Product>> page(ProductQueryDTO query){
        return Result.success(productService.pageQuery(query));
    }

    /**修改商品状态。*/
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status){
        productService.updateStatus(id, status);
        return Result.success();
    }

}
