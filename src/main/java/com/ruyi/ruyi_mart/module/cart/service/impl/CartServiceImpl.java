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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**购物车业务的具体实现。全部存在 Redis 里，key 按"登录用 userId、未登录用游客标识"区分。*/
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
    /**购物车保留时长：每次读写都会续期，长期不动的购物车自动回收，避免 Redis 只增不减*/
    private static final Duration CART_TTL = Duration.ofDays(30);
    /**单个商品的数量上下限*/
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 99;
    /**游客标识的格式：前端生成的是 g_<时间戳>_<随机串>，只放行字母数字与下划线短横，长度 8~64*/
    private static final Pattern GUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    /**
     * 判断用哪个 redis key。
     * 未登录时必须带一个合法的游客标识：以前这里是直接把 guestId 拼上去的，
     * 没带这个头就会拼成 ruyi:cart:guest:null ——
     * 所有"未登录且没带标识"的请求共用同一个购物车，互相能看到对方加的商品。
     */
    private String keyfor(Long userId,String guestId){
        if(userId != null){
            return CART_PREFIX + userId;
        }
        if(guestId == null || !GUEST_ID_PATTERN.matcher(guestId).matches()){
            throw new BusinessException(ResultCode.FAIL, "缺少有效的游客标识（请求头 X-Guest-Id）");
        }
        return GUEST_PREFIX + guestId;
    }

    /**取购物车并续期*/
    private RMap<String,String> cartOf(String key){
        RMap<String,String> cart = redissonClient.getMap(key, StringCodec.INSTANCE);
        cart.expire(CART_TTL);
        return cart;
    }

    /**购物车加购*/
    @Override
    public void addItem(Long userId, String guestId, Long productId, Integer quantity){
        checkQuantity(quantity);
        String key = keyfor(userId,guestId);
        RMap<String,String> cart = cartOf(key);
        String existing = cart.get(String.valueOf(productId));
        CartItemVO item;
        int target;
        if(existing != null){
            item = deserialize(existing);
            target = item.getQuantity() + quantity;
        }else {
            item = loadSnapshot(productId);
            target = quantity;
        }
        if(target > MAX_QUANTITY){
            throw new BusinessException(ResultCode.FAIL, "单个商品最多买 " + MAX_QUANTITY + " 件");
        }
        item.setQuantity(target);
        cart.put(String.valueOf(productId),serialize(item));
    }

    /**修改购物车商品数量*/
    @Override
    public void updateQuantity(Long userId, String guestId, Long productId, Integer quantity){
        checkQuantity(quantity);
        String key = keyfor(userId,guestId);
        RMap<String,String> cart = cartOf(key);
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
        RMap<String,String> cart = cartOf(key);
        cart.remove(String.valueOf(productId));
    }

    /**查看购物车（实时算小计）*/
    @Override
    public List<CartItemVO> list(Long userId, String guestId){
        String key = keyfor(userId,guestId);
        RMap<String,String> cart = cartOf(key);
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

    /**
     * 下单成功后清空购物车。
     * 注意这里必须和别处一样带上 StringCodec：Redisson 是用 codec 把 key 编码成 Redis 里的
     * 二进制 key 的，不带 codec 会走默认 codec，编出来的 key 根本不是一个，
     * 结果就是"删了但没删掉"——购物车看着一直有货。
     */
    @Override
    public void clearKeepStock(Long userId, String guestId){
        redissonClient.getMap(keyfor(userId,guestId), StringCodec.INSTANCE).delete();
    }

    /**数量上下限校验：0、负数、超大值都不该进购物车*/
    private void checkQuantity(Integer quantity){
        if(quantity == null || quantity < MIN_QUANTITY || quantity > MAX_QUANTITY){
            throw new BusinessException(ResultCode.FAIL,
                    "数量必须在 " + MIN_QUANTITY + " ~ " + MAX_QUANTITY + " 之间");
        }
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

    /**合并购物车：把游客购物车并进用户购物车，然后删掉游客那份*/
    @Override
    public void mergeGuestToUser(String guestId,Long userId){
        //合并前先校验一次游客标识，避免把非法 id 当 key 用
        String guestKey = keyfor(null, guestId);
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
                int merged = Math.min(userItem.getQuantity() + guestItem.getQuantity(), MAX_QUANTITY);
                userItem.setQuantity(merged);
                userCart.put(productId,serialize(userItem));
            }else {
                userCart.put(productId, entry.getValue());
            }
        }
        userCart.expire(CART_TTL);
        guestCart.delete();
    }
}
