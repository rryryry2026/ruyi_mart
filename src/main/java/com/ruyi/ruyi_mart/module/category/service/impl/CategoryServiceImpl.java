package com.ruyi.ruyi_mart.module.category.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.util.CaffeineUtils;
import com.ruyi.ruyi_mart.module.category.entity.Category;
import com.ruyi.ruyi_mart.module.category.mapper.CategoryMapper;
import com.ruyi.ruyi_mart.module.category.service.CategoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private CaffeineUtils caffeineUtils;

    public List<Category> getCategoryTreeCache(){
        List<Category> all = lambdaQuery()
                .eq(Category::getStatus,1)
                .orderByAsc(Category::getSort)
                .list();

        List<Category> roots = all.stream()
                .filter(c -> c.getParentId() == 0L)
                .collect(Collectors.toList());

        buildTree(all,roots);
        return  roots;
    }

    @Override
    public List<Category> getCategoryTree(){
        return caffeineUtils.getCategoryTree();
    }

    private void buildTree(List<Category> all,List<Category> parents){
        if(parents == null || parents.isEmpty()){
            return;
        }
        Map<Long,List<Category>> group =
                all.stream().collect(Collectors.groupingBy(Category::getParentId));
        List<Category> nextLevel = new ArrayList<>();
        for(Category parent:parents){
            List<Category> children = group.getOrDefault(parent.getId(),new ArrayList<>());
            parent.setChildren(children);
            nextLevel.addAll(children);
        }
        buildTree(all,nextLevel);
    }

    @Override
    public List<Category> getCategoryChildren(Long categoryId){
        return lambdaQuery()
                .eq(Category::getParentId,categoryId)
                .eq(Category::getStatus,1)
                .orderByAsc(Category::getSort)
                .list();
    }

    @Override
    public void addCategory(Category category){
        if(category.getParentId() == null){
            category.setParentId(0L);
        }
        save(category);
        caffeineUtils.invalidateCategoryTree();
    }

    @Override
    public void deleteCategory(Long id){
        removeById(id);
        caffeineUtils.invalidateCategoryTree();
    }

    @Override
    public void updateCategoryInfo(Long id,Category category){
        category.setId(id);
        updateById(category);
        caffeineUtils.invalidateCategoryTree();
    }

    @Override
    public void updateCategoryStatus(Long id, Integer status){
        lambdaUpdate()
                .eq(Category::getId,id)
                .set(Category::getStatus,status)
                .update();
        caffeineUtils.invalidateCategoryTree();
    }
}
