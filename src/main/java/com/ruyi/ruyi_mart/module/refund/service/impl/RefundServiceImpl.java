package com.ruyi.ruyi_mart.module.refund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.entity.OrderItem;
import com.ruyi.ruyi_mart.module.order.enums.OrderStatus;
import com.ruyi.ruyi_mart.module.order.mapper.OrderItemMapper;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.refund.entity.Refund;
import com.ruyi.ruyi_mart.module.refund.enums.RefundStatus;
import com.ruyi.ruyi_mart.module.refund.mapper.RefundMapper;
import com.ruyi.ruyi_mart.module.refund.service.RefundService;
import com.ruyi.ruyi_mart.module.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RefundServiceImpl extends ServiceImpl<RefundMapper, Refund> implements RefundService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private StockService stockService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Refund apply(Long userId, Long orderId, String reason){
        Order order = orderMapper.selectById(orderId);
        if(order == null){
            throw new BusinessException(ResultCode.NOT_FIND, "订单不存在");
        }
        if(!order.getUserId().equals(userId)){
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该订单");
        }
        Integer status = order.getStatus();
        boolean payable = status == OrderStatus.PAID.getCode()
                || status == OrderStatus.SHIPPED.getCode()
                || status == OrderStatus.COMPLETED.getCode();
        if(!payable){
            throw new BusinessException(ResultCode.FAIL, "只有已支付/已发货/已完成的订单才能申请退款");
        }

        QueryWrapper<Refund> qw = new QueryWrapper<>();
        qw.eq("order_id",orderId)
                .in("status", RefundStatus.PENDING.getCode(),RefundStatus.REFUNDED.getCode());
        if(baseMapper.selectCount(qw) > 0 ){
            throw new BusinessException(ResultCode.FAIL, "该订单已有进行中的退款申请");
        }

        Refund refund = new Refund();
        refund.setOrderId(orderId);
        refund.setUserId(userId);
        refund.setRefundNo(generateRefundNo());
        refund.setAmount(order.getTotalAmount());
        refund.setReason(reason);
        refund.setStatus(RefundStatus.PENDING.getCode());
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(refund);
        return refund;
    }

    @Override
    public List<Refund> listByUser(Long userId){
        QueryWrapper<Refund> qw = new QueryWrapper<>();
        qw.eq("user_id",userId).orderByDesc("create_time");
        return baseMapper.selectList(qw);
    }

    @Override
    public Refund detail(Long userId, Long refundId){
        Refund refund = baseMapper.selectById(refundId);
        if(refund == null){
            throw new BusinessException(ResultCode.NOT_FIND, "退款单不存在");
        }
        if(!refund.getUserId().equals(userId)){
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该退款单");
        }
        return refund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Refund approve(Long refundId){
        Refund refund = baseMapper.selectById(refundId);
        if(refund == null){
            throw new BusinessException(ResultCode.NOT_FIND, "退款单不存在");
        }
        if(refund.getStatus() != RefundStatus.PENDING.getCode()){
            throw new BusinessException(ResultCode.FAIL, "只有待审核的退款单才能审核");
        }
        Long orderId = refund.getOrderId();
        QueryWrapper<OrderItem> itemQw = new QueryWrapper<>();
        itemQw.eq("order_id",orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemQw);
        for(OrderItem item : items){
            stockService.refund(item.getProductId(),item.getQuantity());
        }
        Order updOrder = new Order();
        updOrder.setId(orderId);
        updOrder.setStatus(OrderStatus.REFUNDED.getCode());
        updOrder.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(updOrder);

        refund.setStatus(RefundStatus.REFUNDED.getCode());
        refund.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(refund);
        return refund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Refund reject(Long refundId, String rejectReason){
        Refund refund = baseMapper.selectById(refundId);
        if(refund == null){
            throw new BusinessException(ResultCode.NOT_FIND, "退款单不存在");
        }
        if(refund.getStatus() != RefundStatus.PENDING.getCode()){
            throw new BusinessException(ResultCode.FAIL, "只有待审核的退款单才能审核");
        }
        refund.setStatus(RefundStatus.REJECTED.getCode());
        refund.setRejectReason(rejectReason);
        refund.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(refund);
        return refund;
    }

    private String generateRefundNo() {
        return "RF" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }


}
