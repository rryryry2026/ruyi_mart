package com.ruyi.ruyi_mart.module.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.order.dto.OrderAdminQueryDTO;
import com.ruyi.ruyi_mart.module.order.dto.OrderCreateDTO;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.vo.OrderAdminVO;
import com.ruyi.ruyi_mart.module.order.vo.OrderVO;
import com.ruyi.ruyi_mart.module.payment.vo.PaymentResult;

import java.util.List;

public interface OrderService extends IService<Order> {

    /**创建订单（dto 携带优惠券等结算信息）*/
    OrderVO createOrder(Long userId, OrderCreateDTO dto);


    /**订单列表*/
    List<OrderVO> listOrders(Long userId);

    /**订单详情*/
    OrderVO getOrderDetail(Long userId, Long oderId);

    /**发起支付，返回支付载体（含支付页链接/二维码），订单保持待支付*/
    PaymentResult payOrder(Long userId, Long orderId, String payType);

    /**支付平台异步回调：确认收款后置订单为已支付，并确认库存*/
    void completePayment(Long orderId);

    /**
     * 个人端模拟支付完成。
     * 项目没有对接真实支付渠道，因此提供这个用户端入口，把"支付成功后回调"这一步显式走完。
     * 与 completePayment 的区别：必须先校验订单归属，只允许确认自己的订单。
     */
    void confirmMockPayment(Long userId, Long orderId);

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

    /**管理端：全量订单分页（订单号/买家/状态/时间范围筛选，不限定买家）*/
    Page<OrderAdminVO> adminListOrders(OrderAdminQueryDTO dto);

    /**管理端：订单详情（管理员视角，不校验订单归属）*/
    OrderAdminVO adminGetOrderDetail(Long orderId);

}
