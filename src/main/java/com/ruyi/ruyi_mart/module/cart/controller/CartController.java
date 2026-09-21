package com.ruyi.ruyi_mart.module.cart.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.cart.service.CartService;
import com.ruyi.ruyi_mart.module.cart.vo.CartItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**购物车对外接口层*/
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /**获取线程用户Id*/
    private Long currentUserId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth != null && auth.getPrincipal() instanceof Long){
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    /**增加购物车*/
    @PostMapping("/add")
    public Result<Void> add(@RequestParam Long productId,
                            @RequestParam Integer quantity,
                            @RequestHeader(value = "X-Guest-Id", required = false) String guestId){
        cartService.addItem(currentUserId(),guestId,productId,quantity);
        return Result.success();
    }

    /**更新购物车商品数量*/
    @PutMapping("/quantity")
    public Result<Void> updateQuantity(@RequestParam Long productId,
                                       @RequestParam Integer quantity,
                                       @RequestHeader(value = "X-Guest-Id",required = false) String guestId){
        cartService.updateQuantity(currentUserId(), guestId, productId, quantity);
        return Result.success();
    }

    /**删除购物车商品*/
    @DeleteMapping("/item/{productId}")
    public Result<Void> removeItem(@PathVariable Long productId,
                                   @RequestHeader(value = "X-Guest-Id", required = false) String guestId){
        cartService.removeItem(currentUserId(), guestId, productId);
        return Result.success();
    }

    /**查看购物车列表*/
    @GetMapping
    public Result<List<CartItemVO>> list(@RequestHeader(value = "X-Guest-Id", required = false) String guestId){
        return Result.success(cartService.list(currentUserId(),guestId));
    }

    /**清空购物车*/
    @DeleteMapping("/clear")
    public Result<Void> clear(@RequestHeader(value = "X-Guest-Id", required = false) String guestId){
        cartService.clear(currentUserId(),guestId);
        return Result.success();
    }

    /**合并购物车*/
    @PostMapping("/merge")
    public Result<Void> merge(@RequestHeader(value = "X-Guest-Id", required = false) String guestId){
        Long userId = currentUserId();
        if(userId != null && guestId != null && !guestId.isEmpty()){
            cartService.mergeGuestToUser(guestId,userId);
        }
        return Result.success();
    }
}
