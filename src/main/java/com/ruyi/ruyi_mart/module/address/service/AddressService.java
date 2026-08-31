package com.ruyi.ruyi_mart.module.address.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.address.entity.Address;

import java.util.List;

public interface AddressService extends IService<Address> {

    //查询全部收货地址
    List<Address> getAddressList(Long userId);

    //新增收货地址
    void insertAddress(Address address,Long userId);

    //更新收货地址
    void updateAddress(Address address, Long userId);

    //删除收货地址
    void deleteAddress(Long id,Long userId);
}
