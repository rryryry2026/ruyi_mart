package com.ruyi.ruyi_mart.module.cart.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.cart.service.CartService;
import com.ruyi.ruyi_mart.module.cart.vo.CartItemVO;
import com.ruyi.ruyi_mart.module.product.entity.Product;
import com.ruyi.ruyi_mart.module.product.service.ProductService;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**购物车业务的具体实现。*/
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedissonClient redissonClient;//操作 Redis 的客户端
    @Autowired
    private ProductService productService;//拉商品信息（做快照用）
    @Autowired
    private ObjectMapper objectMapper;//JSON ↔ 对象 的转换器

    private static final String CART_PREFIX = "ruyi:cart:";
    private static final String GUEST_PREFIX = "ruyi:cart:guest:";

    /**判断用哪个redis key*/
    private String keyfor(Long userId,String guestId){
        return userId != null ? CART_PREFIX + userId : GUEST_PREFIX + guestId;
    }

    /**购物车加购*/
    @Override
    public void addItem(Long userId, String guestId, Long productId, Integer quantity){

        String key = keyfor(userId,guestId);
        RMap<String,String> cart = redissonClient.getMap(key, StringCodec.INSTANCE);
        String existing = cart.get(String.valueOf(productId));
        CartItemVO item;
        if(existing != null){
            item = deserialize(existing);
            item.setQuantity(item.getQuantity() + quantity);
        }else {
            item = loadSnapshot(productId);
            item.setQuantity(quantity);
        }
        cart.put(String.valueOf(productId),serialize(item));

    }

    /**修改购物车商品数量*/
    @Override
    public void updateQuantity(Long userId, String guestId, Long productId, Integer quantity){
        String key = keyfor(userId,guestId);
        RMap<String,String> cart = redissonClient.getMap(key, StringCodec.INSTANCE);
        String existing = cart.get(String.valueOf(productId));
        if(existing == null) return;
        CartItemVO item = deserialize(existing);
        item.setQuantity(quantity);
        cart.put(String.valueOf(productId),serialize(item));
    }

    /**删掉购物车的某一商品*/
    @Override
    public void removeItem(Long userId, String guestId, Long productId){
        String key = keyfor(userId,guestId);
        RMap<String,String> cart = redissonClient.getMap(key,StringCodec.INSTANCE);
        cart.remove(String.valueOf(productId));
    }

    /**查看购物车（实时算小计）*/
    @Override
    public List<CartItemVO> list(Long userId, String guestId){
        String key = keyfor(userId,guestId);
        RMap<String,String> cart = redissonClient.getMap(key,StringCodec.INSTANCE);
        List<CartItemVO> result = new ArrayList<>();
        for(String json : cart.values()){
            CartItemVO item = deserialize(json);
            item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            result.add(item);
        }
        return result;
    }

    /**清空购物车*/
    @Override
    public void clear(Long userId,String guestId){
        String key = keyfor(userId,guestId);
        redissonClient.getMap(key,StringCodec.INSTANCE).delete();
    }

    @Override
    public void clearKeepStock(Long userId, String guestId){
        redissonClient.getMap(keyfor(userId,guestId)).delete();
    }

    /**获取商品快照*/
    private CartItemVO loadSnapshot(Long productId){
        Product p = productService.getById(productId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FIND, "商品不存在: " + productId);
        }
        CartItemVO vo = new CartItemVO();
        vo.setProductId(p.getId());
        vo.setName(p.getName());
        vo.setPrice(p.getPrice());
        vo.setImageUrl(p.getImageUrl());
        vo.setQuantity(0);
        return vo;
    }

    /**序列化 对象 → JSON 字符串*/
    private String serialize(CartItemVO v){
        try {
            return objectMapper.writeValueAsString(v);
        }catch (Exception e){
            throw new RuntimeException("购物车序列化失败:",e);
        }
    }

    /**反序列化 JSON → 对象*/
    private CartItemVO deserialize(String json){
        try {
            return objectMapper.readValue(json, CartItemVO.class);
        }catch (JsonProcessingException e){
            throw new RuntimeException("购物车反序列化失败:",e);
        }
    }

    /**合并购物车*/
    @Override
    public void mergeGuestToUser(String guestId,Long userId){
        String guestKey = GUEST_PREFIX + guestId;
        String userKey = CART_PREFIX + userId;
        RMap<String,String> guestCart = redissonClient.getMap(guestKey,StringCodec.INSTANCE);
        if(guestCart.isEmpty()) return;
        RMap<String,String> userCart = redissonClient.getMap(userKey,StringCodec.INSTANCE);
        for(Map.Entry<String,String> entry : guestCart.entrySet()){
            String productId = entry.getKey();
            CartItemVO guestItem = deserialize(entry.getValue());
            String existing = userCart.get(productId);
            if(existing != null){
                CartItemVO userItem = deserialize(existing);
                userItem.setQuantity(userItem.getQuantity() + guestItem.getQuantity());
                userCart.put(productId,serialize(userItem));
            }else {
                userCart.put(productId, entry.getValue());
            }
        }
        guestCart.delete();
    }
}
