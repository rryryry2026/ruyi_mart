package com.ruyi.ruyi_mart.common.util;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ruyi.ruyi_mart.common.constant.CategoryCacheConstant;
import com.ruyi.ruyi_mart.module.category.entity.Category;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**分类树本地缓存。*/
@Component
public class CaffeineUtils {

    /**自带加载能力。*/
    @Resource
    private LoadingCache<String, List<Category>> categoryTreeCache;

    /**拿分类树*/
    public List<Category> getCategoryTree(){
        try {
            return categoryTreeCache.get(CategoryCacheConstant.CACHE_KEY_CATEGORY_TREE);
        }catch (Exception e){
            throw new RuntimeException("加载分类树缓存失败", e);
        }
    }

    /**清除缓存。*/
    public void invalidateCategoryTree(){
        categoryTreeCache.invalidate(CategoryCacheConstant.CACHE_KEY_CATEGORY_TREE);
    }
}
