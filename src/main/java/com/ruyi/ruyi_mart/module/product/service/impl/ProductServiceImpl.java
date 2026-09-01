package com.ruyi.ruyi_mart.module.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.ruyi_mart.module.product.dto.ProductQueryDTO;
import com.ruyi.ruyi_mart.module.product.entity.Product;
import com.ruyi.ruyi_mart.module.product.mapper.ProductMapper;
import com.ruyi.ruyi_mart.module.product.service.ProductService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;


@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "ruyi:product:detail:";
    private static final long CACHE_TTL_SECONDS = 30 * 60;

    @Override
    public Product getById(Serializable id){
        String key = KEY_PREFIX + id;
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        String cached = bucket.get();
        if(cached != null){
            return deserialize(cached);
        }
        Product product = super.getById(id);
        if(product != null){
            bucket.set(serialize(product),CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return product;
    }

    @Override
    public boolean updateById(Product entity){
        boolean r = super.updateById(entity);
        redissonClient.getBucket(KEY_PREFIX + entity.getId()).delete();
        return r;
    }

    @Override
    public boolean removeById(Serializable id){
        boolean r = super.removeById(id);
        redissonClient.getBucket(KEY_PREFIX + id).delete();
        return r;
    }

    @Override
    public Page<Product> pageQuery(ProductQueryDTO q){
        int pageNum = (q.getPageNum() == null || q.getPageNum() < 1) ? 1 :q.getPageNum();
        int pageSize = (q.getPageSize() == null || q.getPageSize() < 1) ? 10 :q.getPageSize();
        Page<Product> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        if(StringUtils.hasText(q.getKeyword())){
            w.like(Product::getName,q.getKeyword());
        }
        if(q.getCategoryId() != null){
            w.eq(Product::getCategoryId,q.getCategoryId());
        }
        if(q.getStatus() != null){
            w.eq(Product::getStatus,q.getStatus());
        }
        w.orderByDesc(Product::getCreateTime);
        return page(page,w);

    }


    private String serialize(Product p){
        try{
            return  objectMapper.writeValueAsString(p);
        }catch (Exception e){
            throw  new RuntimeException("商品序列化失败",e);
        }
    }

    private Product deserialize(String json){
        try{
            return objectMapper.readValue(json, Product.class);
        }catch (JsonProcessingException e){
            throw  new RuntimeException("商品反序列化失败",e);
        }
    }
}
