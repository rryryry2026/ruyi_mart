package com.ruyi.ruyi_mart.module.category.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.category.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /** 获取完整分类树*/
    List<Category> getCategoryTree();

    /** 获取某个分类的直接子节点 */
    List<Category> getCategoryChildren(Long categoryId);

    /** 新增分类 */
    void addCategory(Category category);

    /** 删除分类 */
    void deleteCategory(Long id);

    /** 修改分类信息 */
    void updateCategoryInfo(Long id, Category category);

    /** 修改分类状态（0-禁用 1-启用） */
    void updateCategoryStatus(Long id, Integer status);
}
