package com.ruyi.ruyi_mart.module.cart.service;

import com.ruyi.ruyi_mart.module.cart.vo.CartItemVO;

import java.util.List;

public interface CartService {

    //加购
    void addItem(Long userId, String guestId, Long productId, Integer quantity);

    //修改数量
    void updateQuantity(Long userId, String guestId, Long productId, Integer quantity);

    //删除
    void removeItem(Long userId, String guestId, Long productId);

    //查看
    List<CartItemVO> list(Long userId, String guestId);

    //清空
    void clear(Long userId, String guestId);

    //清空但不释放库存
    void clearKeepStock(Long userId, String guestId);

    //合并
    void mergeGuestToUser(String guestId, Long userId);



}
