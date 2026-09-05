package com.ruyi.ruyi_mart.module.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.vo.OrderVO;
import com.ruyi.ruyi_mart.module.payment.vo.PaymentResult;

import java.util.List;

public interface OrderService extends IService<Order> {

    /**创建订单*/
    OrderVO createOrder(Long userId);

    /**订单列表*/
    List<OrderVO> listOrders(Long userId);

    /**订单详情*/
    OrderVO getOrderDetail(Long userId, Long oderId);

    /**发起支付，返回支付载体（含支付页链接/二维码），订单保持待支付*/
    PaymentResult payOrder(Long userId, Long orderId, String payType);

    /**支付平台异步回调：确认收款后置订单为已支付，并确认库存*/
    void completePayment(Long orderId);

    /**取消订单*/
    OrderVO cancelOrder(Long userId, Long orderId);

    /**按状态筛选订单*/
    List<OrderVO> listOrdersByStatus(Long userId, Integer status);

    /**分页查询订单*/
    Page<OrderVO> listOrdersPage(Long userId, Integer status, int pageNum, int pageSize);

    /**管理员发货*/
    OrderVO shipOrder(Long orderId);

    /**用户确认收货*/
    OrderVO confirmReceive(Long userId, Long orderId);


}
