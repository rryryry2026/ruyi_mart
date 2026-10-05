package com.ruyi.ruyi_mart.module.address.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.address.entity.Address;
import com.ruyi.ruyi_mart.module.address.mapper.AddressMapper;
import com.ruyi.ruyi_mart.module.address.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**收货地址业务的真正实现。*/
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper,Address> implements AddressService {

    /**查所有地址列表。*/
    @Override
    public List<Address> getAddressList(Long userId) {
        return lambdaQuery()
                .eq(Address::getUserId,userId)
                .orderByDesc(Address::getIsDefault)
                .orderByAsc(Address::getId)
                .list();
    }

    /**新增地址。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertAddress(Address address, Long userId){
        address.setUserId(userId);
        if(address.getIsDefault() != null && address.getIsDefault() == 1){
            clearOtherDefault(userId, null);
        }
        save(address);
    }

    /**修改地址。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Address address,Long userId){
        Long owned = lambdaQuery()
                .eq(Address::getId,address.getId())
                .eq(Address::getUserId,userId)
                .count();
        if(owned == 0){
            throw new BusinessException(ResultCode.FAIL,"地址不存在或不属于当前用户");
        }
        address.setUserId(userId);
        if(address.getIsDefault() != null && address.getIsDefault() == 1){
            clearOtherDefault(userId, address.getId());
        }
        updateById(address);
    }

    /**删除地址。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long id, Long userId){
        boolean removed = lambdaUpdate()
                .eq(Address::getId,id)
                .eq(Address::getUserId,userId)
                .remove();
        if(!removed){
            throw new BusinessException(ResultCode.FAIL,"地址不存在或不属于当前用户");
        }
    }

    /**清除其他默认地址。*/
    private void clearOtherDefault(Long userId, Long excludeAddressId){
        // 只动"当前是默认"的行、并排除本次要设默认的地址本身：
        // 原来把该用户所有地址整批刷一遍，非默认地址的 update_time 也被无意义地刷新
        lambdaUpdate()
                .eq(Address::getUserId,userId)
                .eq(Address::getIsDefault,1)
                .ne(excludeAddressId != null, Address::getId, excludeAddressId)
                .set(Address::getIsDefault,0)
                .update();
    }
}
