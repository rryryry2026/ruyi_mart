package com.ruyi.ruyi_mart.module.refund.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.module.refund.entity.Refund;
import com.ruyi.ruyi_mart.module.refund.vo.RefundAdminVO;

import java.util.List;

public interface RefundService {

    /**用户申请退款*/
    Refund apply(Long userId,Long orderId,String reason);

    /**查看退款订单列表*/
    List<Refund> listByUser(Long userId);

    /**查看退款订单详情*/
    Refund detail(Long userId,Long refundId);

    /**管理员审核成功并退款*/
    Refund approve(Long refundId);

    /**管理员拒绝*/
    Refund reject(Long refundId,String rejectReason);

    /**管理端：全量退款单分页（不限定申请人），带关联订单号与买家昵称*/
    Page<RefundAdminVO> adminPageRefunds(Integer status, int pageNum, int pageSize);

}
