package com.ruyi.ruyi_mart.module.category.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.category.entity.Category;
import com.ruyi.ruyi_mart.module.category.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/tree")
    public Result<List<Category>> getCategoryTree(){
        return Result.success(categoryService.getCategoryTree());
    }

    @GetMapping("/children")
    public Result<List<Category>> getCategoryChildren(@RequestParam Long categoryId){
        return  Result.success(categoryService.getCategoryChildren(categoryId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> addCategory(@RequestBody Category category){
        categoryService.addCategory(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCategory(@PathVariable Long id){
        categoryService.deleteCategory(id);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateCategoryInfo(@PathVariable Long id, @RequestBody Category category){
        categoryService.updateCategoryInfo(id, category);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateCategoryStatus(@PathVariable Long id, @RequestParam Integer status){
        categoryService.updateCategoryStatus(id, status);
        return Result.success();
    }

}
