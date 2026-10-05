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

/**创建分类树的caffeine缓存，规定没命中如何加载。*/
@Configuration
public class CaffeineConfig {

    /**注入service。*/
    @Lazy
    @Autowired
    private CategoryServiceImpl categoryServiceImpl;

    /**创建并返回LoadingCache Bean 同时装加载函数。*/
    @Bean
    public LoadingCache<String, List<Category>> categoryTreeCache(){
        return Caffeine.<String,List<Category>>newBuilder()
                .initialCapacity(1)
                .maximumSize(1)
                // 必须有 TTL：某个写路径漏调 invalidate 时，脏数据只活 10 分钟，
                // 而不是一直脏到应用重启。写路径正常时到期重载也只是一次 DB 查询
                .expireAfterWrite(java.time.Duration.ofMinutes(10))
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
