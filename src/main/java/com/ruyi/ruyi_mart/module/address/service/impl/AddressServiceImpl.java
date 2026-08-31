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

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper,Address> implements AddressService {

    @Override
    public List<Address> getAddressList(Long userId) {
        return lambdaQuery()
                .eq(Address::getUserId,userId)
                .orderByDesc(Address::getIsDefault)
                .orderByAsc(Address::getId)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertAddress(Address address, Long userId){
        address.setUserId(userId);
        if(address.getIsDefault() != null && address.getIsDefault() == 1){
            clearOtherDefault(userId);
        }
        save(address);
    }

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
            clearOtherDefault(userId);
        }
        updateById(address);
    }

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

    private void clearOtherDefault(Long userId){
        lambdaUpdate()
                .eq(Address::getUserId,userId)
                .set(Address::getIsDefault,0)
                .update();
    }
}
