package com.ruyi.ruyi_mart.module.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.vo.OrderVO;

public interface OrderService extends IService<Order> {

    OrderVO createOrder(Long userId);
}
