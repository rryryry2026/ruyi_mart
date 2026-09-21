package com.ruyi.ruyi_mart.module.cart.service;

import com.ruyi.ruyi_mart.module.cart.vo.CartItemVO;

import java.util.List;

/**购物车业务接口，声明方法。*/
public interface CartService {

    /**增加购物车*/
    void addItem(Long userId, String guestId, Long productId, Integer quantity);

    /**修改购物车商品数量*/
    void updateQuantity(Long userId, String guestId, Long productId, Integer quantity);

    /**删除购物车*/
    void removeItem(Long userId, String guestId, Long productId);

    /**查看购物车*/
    List<CartItemVO> list(Long userId, String guestId);

    /**清空购物车*/
    void clear(Long userId, String guestId);

    /**清空但不释放库存*/
    void clearKeepStock(Long userId, String guestId);

    /**合并购物车*/
    void mergeGuestToUser(String guestId, Long userId);



}
