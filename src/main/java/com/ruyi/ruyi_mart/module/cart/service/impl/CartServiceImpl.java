package com.ruyi.ruyi_mart.module.cart.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.common.result.Result;
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


@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private ProductService productService;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String CART_PREFIX = "ruyi:cart:";
    private static final String GUEST_PREFIX = "ruyi:cart:guest:";

    private String keyfor(Long userId,String guestId){
        return userId != null ? CART_PREFIX + userId : GUEST_PREFIX + guestId;
    }

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

    @Override
    public void removeItem(Long userId, String guestId, Long productId){
        String key = keyfor(userId,guestId);
        RMap<String,String> cart = redissonClient.getMap(key,StringCodec.INSTANCE);
        cart.remove(String.valueOf(productId));
    }

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

    @Override
    public void clear(Long userId,String guestId){
        redissonClient.getMap(keyfor(userId,guestId)).delete();
    }

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

    private String serialize(CartItemVO v){
        try {
            return objectMapper.writeValueAsString(v);
        }catch (Exception e){
            throw new RuntimeException("购物车序列化失败:",e);
        }
    }

    private CartItemVO deserialize(String json){
        try {
            return objectMapper.readValue(json, CartItemVO.class);
        }catch (JsonProcessingException e){
            throw new RuntimeException("购物车反序列化失败:",e);
        }
    }

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
