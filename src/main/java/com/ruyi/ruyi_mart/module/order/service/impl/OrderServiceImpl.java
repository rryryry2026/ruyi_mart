package com.ruyi.ruyi_mart.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.cart.service.CartService;
import com.ruyi.ruyi_mart.module.cart.vo.CartItemVO;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.entity.OrderItem;
import com.ruyi.ruyi_mart.module.order.mapper.OrderItemMapper;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.order.mq.OrderEventProducer;
import com.ruyi.ruyi_mart.module.order.service.OrderService;
import com.ruyi.ruyi_mart.module.order.vo.OrderVO;
import com.ruyi.ruyi_mart.module.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private CartService cartService;
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderEventProducer orderEventProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId){
        List<CartItemVO> cartItems = cartService.list(userId,null);
        if(cartItems == null || cartItems.isEmpty()){
            throw new BusinessException(ResultCode.NOT_FIND,"购物车为空，无法下单");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStatus(0);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> itemList = new ArrayList<>();
        for(CartItemVO ci : cartItems){
            Long productId = ci.getProductId();
            Integer quantity = ci.getQuantity();

            BigDecimal subtotal = ci.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(productId);
            item.setProductName(ci.getName());
            item.setPrice(ci.getPrice());
            item.setQuantity(ci.getQuantity());
            item.setSubtotal(subtotal);
            itemList.add(item);

        }

        order.setTotalAmount(totalAmount);
        baseMapper.updateById(order);

        for(OrderItem item : itemList){
            orderItemMapper.insert(item);
        }

        cartService.clear(userId,null);

        orderEventProducer.sendOrderCreated(order.getId());

        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(itemList);
        return vo;
    }

    @Override
    public List<OrderVO> listOrders(Long userId){
        QueryWrapper<Order> qw = new QueryWrapper<>();
        qw.eq("user_id",userId).orderByDesc("create_time");
        List<Order> orders = baseMapper.selectList(qw);
        List<OrderVO> result = new ArrayList<>();
        for(Order o :orders){
            result.add(toVO(o));
        }
        return result;
    }

    @Override
    public OrderVO getOrderDetail(Long userId, Long orderId){
        Order order = baseMapper.selectById(orderId);
        if(order == null){
            throw new BusinessException(ResultCode.NOT_FIND,"订单不存在");
        }
        if(!order.getUserId().equals(userId)){
            throw new BusinessException(ResultCode.FORBIDDEN,"无权查看该订单");
        }
        return toVO(order);
    }

    private OrderVO toVO(Order order){
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        QueryWrapper<OrderItem> qw = new QueryWrapper<>();
        qw.eq("order_id",order.getId());
        vo.setItems(orderItemMapper.selectList(qw));
        return vo;
    }

    private String generateOrderNo(){
        return System.currentTimeMillis() + UUID.randomUUID().toString().replace("-","").substring(0,6);
    }

}
