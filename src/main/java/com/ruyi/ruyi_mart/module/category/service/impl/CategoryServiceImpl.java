package com.ruyi.ruyi_mart.module.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.common.util.CaffeineUtils;
import com.ruyi.ruyi_mart.module.category.entity.Category;
import com.ruyi.ruyi_mart.module.category.mapper.CategoryMapper;
import com.ruyi.ruyi_mart.module.category.service.CategoryService;
import com.ruyi.ruyi_mart.module.product.entity.Product;
import com.ruyi.ruyi_mart.module.product.mapper.ProductMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private CaffeineUtils caffeineUtils;

    @Autowired
    private ProductMapper productMapper;//商品表：删除分类前检查是否还挂着商品

    /**从数据库中查已启用的分类，组装成树。*/
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

    /**委托，获取分类树。（缓存发生的地方）*/
    @Override
    public List<Category> getCategoryTree(){
        return caffeineUtils.getCategoryTree();
    }

    /**把分类按parentId组装成树。（递归）*/
    private void buildTree(List<Category> all,List<Category> parents){
        if(parents == null || parents.isEmpty()){
            return;
        }
        // 分组只算一次：原来每层递归都对全表重新 groupingBy，深度 d、数量 n 时是 O(n×d)
        Map<Long,List<Category>> group =
                all.stream().collect(Collectors.groupingBy(Category::getParentId));
        linkChildren(group,parents);
    }

    /**按预分好的组把 children 挂到各自的父节点上，再向下一层递归。*/
    private void linkChildren(Map<Long,List<Category>> group,List<Category> parents){
        List<Category> nextLevel = new ArrayList<>();
        for(Category parent:parents){
            List<Category> children = group.getOrDefault(parent.getId(),new ArrayList<>());
            parent.setChildren(children);
            nextLevel.addAll(children);
        }
        if(!nextLevel.isEmpty()){
            linkChildren(group,nextLevel);
        }
    }

    /**获取商品某分类下的子分类。*/
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
        // 有子分类或挂着商品时禁止删除：直接删会让子分类变孤儿
        // （parentId 还指着已删的父分类，导航树上整块消失），商品则挂到一个不存在的分类上
        long children = this.lambdaQuery().eq(Category::getParentId,id).count();
        if(children > 0){
            throw new BusinessException(ResultCode.FAIL,"该分类下还有 " + children + " 个子分类，请先处理子分类");
        }
        long products = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId,id));
        if(products > 0){
            throw new BusinessException(ResultCode.FAIL,"该分类下还有 " + products + " 个商品，请先移走商品");
        }
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
        if(status == null || (status != 0 && status != 1)){
            throw new BusinessException(ResultCode.FAIL,"status 必须为 0 或 1");
        }
        boolean updated = lambdaUpdate()
                .eq(Category::getId,id)
                .set(Category::getStatus,status)
                .set(Category::getUpdateTime, LocalDateTime.now())
                .update();
        // 影响行数兜底：改不存在的记录不该静默返回成功
        if(!updated){
            throw new BusinessException(ResultCode.NOT_FIND,"分类不存在");
        }
        caffeineUtils.invalidateCategoryTree();
    }
}
