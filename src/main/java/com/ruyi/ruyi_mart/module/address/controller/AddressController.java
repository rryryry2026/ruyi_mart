package com.ruyi.ruyi_mart.module.address.controller;

import jakarta.validation.Valid;
import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.address.entity.Address;
import com.ruyi.ruyi_mart.module.address.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**收货地址管理的接口层，身份信息只从token上取，杜绝水平越权漏洞。*/
@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    /**
     * 获取登录Id。
     * spring security的过滤器负责‘存入’和‘请求结束后清除‘。
     */
    private Long currentUserId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth != null && auth.getPrincipal() instanceof Long){
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    /**查列表。*/
    @GetMapping("/list")
    public Result<List<Address>> list(){

        return Result.success(addressService.getAddressList(currentUserId()));
    }

    /**新增地址。*/
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody Address address){
        addressService.insertAddress(address,currentUserId());
        return Result.success();
    }

    /**修改地址。*/
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody Address address){
        addressService.updateAddress(address,currentUserId());
        return Result.success();
    }

    /**删除地址。*/
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam Long id){
        addressService.deleteAddress(id,currentUserId());
        return Result.success();
    }
}
