package com.ruyi.ruyi_mart.module.cart.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.cart.service.CartService;
import com.ruyi.ruyi_mart.module.cart.vo.CartItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    private Long currentUserId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth != null && auth.getPrincipal() instanceof Long){
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestParam Long productId,
                            @RequestParam Integer quantity,
                            @RequestHeader(value = "X-Guest-Id", required = false) String guestId){
        cartService.addItem(currentUserId(),guestId,productId,quantity);
        return Result.success();
    }

    @PutMapping("/quantity")
    public Result<Void> updateQuantity(@RequestParam Long productId,
                                       @RequestParam Integer quantity,
                                       @RequestHeader(value = "X-Guest-Id",required = false) String guestId){
        cartService.updateQuantity(currentUserId(), guestId, productId, quantity);
        return Result.success();
    }

    @DeleteMapping("/item/{productId}")
    public Result<Void> removeItem(@PathVariable Long productId,
                                   @RequestHeader(value = "X-Guest-Id", required = false) String guestId){
        cartService.removeItem(currentUserId(), guestId, productId);
        return Result.success();
    }

    @GetMapping
    public Result<List<CartItemVO>> list(@RequestHeader(value = "X-Guest-Id", required = false) String guestId){
        return Result.success(cartService.list(currentUserId(),guestId));
    }

    @DeleteMapping("/clear")
    public Result<Void> clear(@RequestHeader(value = "X-Guest-Id", required = false) String guestId){
        cartService.clear(currentUserId(),guestId);
        return Result.success();
    }

    @PostMapping("/merge")
    public Result<Void> merge(@RequestHeader(value = "X-Guest-Id", required = false) String guestId){
        Long userId = currentUserId();
        if(userId != null && guestId != null && !guestId.isEmpty()){
            cartService.mergeGuestToUser(guestId,userId);
        }
        return Result.success();
    }
}
