package com.ruyi.ruyi_mart.module.address.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.address.entity.Address;
import com.ruyi.ruyi_mart.module.address.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    private Long currentUserId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth != null && auth.getPrincipal() instanceof Long){
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    @GetMapping("/list")
    public Result<List<Address>> list(){
        return Result.success(addressService.getAddressList(currentUserId()));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody Address address){
        addressService.insertAddress(address,currentUserId());
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Address address){
        addressService.updateAddress(address,currentUserId());
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam Long id){
        addressService.deleteAddress(id,currentUserId());
        return Result.success();
    }
}
