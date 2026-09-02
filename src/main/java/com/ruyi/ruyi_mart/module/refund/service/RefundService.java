package com.ruyi.ruyi_mart.module.refund.service;

import com.ruyi.ruyi_mart.module.refund.entity.Refund;

import java.util.List;

public interface RefundService {

    //用户申请退款
    Refund apply(Long userId,Long orderId,String reason);

    //查看退款订单列表
    List<Refund> listByUser(Long userId);

    //查看退款订单详情
    Refund detail(Long userId,Long refundId);

    //管理员审核成功并退款
    Refund approve(Long refundId);

    //管理员拒绝
    Refund reject(Long refundId,String rejectReason);


}
