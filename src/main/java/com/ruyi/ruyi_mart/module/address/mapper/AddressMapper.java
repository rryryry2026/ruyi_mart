package com.ruyi.ruyi_mart.module.address.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.address.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**收货地址的mapper层。*/
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
