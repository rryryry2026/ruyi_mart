package com.ruyi.ruyi_mart.module.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.vo.OrderVO;

import java.util.List;

public interface OrderService extends IService<Order> {

    OrderVO createOrder(Long userId);

    List<OrderVO> listOrders(Long userId);

    OrderVO getOrderDetail(Long userId, Long oderId);
}
