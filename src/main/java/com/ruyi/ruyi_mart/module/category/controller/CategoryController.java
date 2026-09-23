package com.ruyi.ruyi_mart.module.category.controller;

import jakarta.validation.Valid;
import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.category.entity.Category;
import com.ruyi.ruyi_mart.module.category.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**商品分类业务模块的接口层。*/
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**查商品分类树。*/
    @GetMapping("/tree")
    public Result<List<Category>> getCategoryTree(){

        return Result.success(categoryService.getCategoryTree());
    }

    /**查商品某个分类的子分类。*/
    @GetMapping("/children")
    public Result<List<Category>> getCategoryChildren(@RequestParam Long categoryId){
        return  Result.success(categoryService.getCategoryChildren(categoryId));
    }

    /**新增商品分类。*/
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> addCategory(@Valid @RequestBody Category category){
        categoryService.addCategory(category);
        return Result.success();
    }

    /**删除分类。*/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCategory(@PathVariable Long id){
        categoryService.deleteCategory(id);
        return Result.success();
    }

    /**修改分类信息。*/
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateCategoryInfo(@PathVariable Long id, @Valid @RequestBody Category category){
        categoryService.updateCategoryInfo(id, category);
        return Result.success();
    }

    /**改分类状态 启用/禁用*/
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateCategoryStatus(@PathVariable Long id, @RequestParam Integer status){
        categoryService.updateCategoryStatus(id, status);
        return Result.success();
    }

}
