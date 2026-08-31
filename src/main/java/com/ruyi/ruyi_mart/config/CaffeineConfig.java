package com.ruyi.ruyi_mart.config;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ruyi.ruyi_mart.common.constant.CategoryCacheConstant;
import com.ruyi.ruyi_mart.module.category.entity.Category;
import com.ruyi.ruyi_mart.module.category.service.impl.CategoryServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

@Configuration
public class CaffeineConfig {

    @Lazy
    @Autowired
    private CategoryServiceImpl categoryServiceImpl;

    @Bean
    public LoadingCache<String, List<Category>> categoryTreeCache(){
        return Caffeine.<String,List<Category>>newBuilder()
                .initialCapacity(1)
                .maximumSize(1)
                .build(new CacheLoader<String,List<Category>>() {
                    @Override
                    public List<Category> load(String key) throws Exception{
                        if(CategoryCacheConstant.CACHE_KEY_CATEGORY_TREE.equals(key)){
                            return categoryServiceImpl.getCategoryTreeCache();
                        }
                        throw new IllegalAccessException("cache 未定义:" + key);
                    }
                });
    }
}
