package com.ruyi.ruyi_mart.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.cart.service.CartService;
import com.ruyi.ruyi_mart.module.cart.vo.CartItemVO;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.entity.OrderItem;
import com.ruyi.ruyi_mart.module.order.enums.OrderStatus;
import com.ruyi.ruyi_mart.module.order.mapper.OrderItemMapper;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.order.mq.OrderEventProducer;
import com.ruyi.ruyi_mart.module.order.service.OrderService;
import com.ruyi.ruyi_mart.module.order.vo.OrderVO;
import com.ruyi.ruyi_mart.module.payment.holder.PaymentStrategyHolder;
import com.ruyi.ruyi_mart.module.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private CartService cartService;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private PaymentStrategyHolder paymentStrategyHolder;
    @Autowired
    private StockService stockService;
    @Autowired
    private OrderEventProducer orderEventProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId){
        List<CartItemVO> cartItems = cartService.list(userId,null);
        if(cartItems == null || cartItems.isEmpty()){
            throw new BusinessException(ResultCode.NOT_FIND,"购物车为空，无法下单");
        }

        List<CartItemVO> sortedItems = new ArrayList<>(cartItems);
        sortedItems.sort(Comparator.comparing(CartItemVO::getProductId));
        for(CartItemVO ci : sortedItems){
            boolean locked = stockService.tryLock(ci.getProductId(), ci.getQuantity());
            if(!locked){
                throw new BusinessException(ResultCode.FAIL,"商品库存不足: " + ci.getName());
            }
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

        cartService.clearKeepStock(userId,null);
        orderEventProducer.sendCloseDelay(order.getId());

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO payOrder(Long userId,Long orderId,String payType){
        Order order = baseMapper.selectById(orderId);
        if(order == null){
            throw new BusinessException(ResultCode.NOT_FIND,"订单不存在");
        }
        if(!order.getUserId().equals(userId)){
            throw new BusinessException(ResultCode.FORBIDDEN,"无权操作该订单");
        }
        if(order.getStatus() != OrderStatus.PENDING.getCode()){
            throw new BusinessException(ResultCode.FAIL,"订单状态异常，无法支付");
        }

        boolean paid = paymentStrategyHolder.get(payType)
                .pay(orderId,userId,order.getTotalAmount());
        if(!paid){
            throw new BusinessException(ResultCode.FAIL,"支付失败");
        }

        QueryWrapper<OrderItem> qw = new QueryWrapper<>();
        qw.eq("order_id",orderId);
        List<OrderItem> items = orderItemMapper.selectList(qw);
        for(OrderItem item : items){
            stockService.confirm(item.getProductId(),item.getQuantity());
        }

        Order upd = new Order();
        upd.setId(orderId);
        upd.setStatus(OrderStatus.PAID.getCode());
        upd.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(upd);
        return getOrderDetail(userId,orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancelOrder(Long userId,Long orderId){
        Order order = baseMapper.selectById(orderId);
        if(order == null){
            throw new BusinessException(ResultCode.NOT_FIND,"订单不存在");
        }
        if(!order.getUserId().equals(userId)){
            throw new BusinessException(ResultCode.FORBIDDEN,"无权操作该订单");
        }
        if(order.getStatus() != OrderStatus.PENDING.getCode()){
            throw new BusinessException(ResultCode.FAIL,"只有待支付订单才能取消");
        }

        QueryWrapper<OrderItem> qw = new QueryWrapper<>();
        qw.eq("order_id",orderId);
        List<OrderItem> items = orderItemMapper.selectList(qw);
        for(OrderItem item:items){
            stockService.release(item.getProductId(), item.getQuantity());
        }

        Order upd = new Order();
        upd.setId(orderId);
        upd.setStatus(OrderStatus.CANCELLED.getCode());
        upd.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(upd);

        return getOrderDetail(userId,orderId);
    }

    @Override
    public List<OrderVO> listOrdersByStatus(Long userId, Integer status){
        QueryWrapper<Order> qw = new QueryWrapper<>();
        qw.eq("user_id",userId).eq("status",status).orderByDesc("create_time");
        List<Order> orders = baseMapper.selectList(qw);
        List<OrderVO> result = new ArrayList<>();
        for(Order o : orders){
            result.add(toVO(o));
        }
        return result;
    }

    @Override
    public Page<OrderVO> listOrdersPage(Long userId,Integer status,int pageNum,int pageSize){
        Page<Order> page = new Page<>(pageNum,pageSize);
        QueryWrapper<Order> qw = new QueryWrapper<>();
        qw.eq("user_id",userId);
        if(status != null){
            qw.eq("status",status);
        }
        qw.orderByDesc("create_time");
        baseMapper.selectPage(page,qw);

        Page<OrderVO> voPage = new Page<>(page.getCurrent(),page.getSize(),page.getTotal());
        List<OrderVO> vos = new ArrayList<>();
        for(Order o :page.getRecords()){
            vos.add(toVO(o));
        }
        voPage.setRecords(vos);
        return voPage;
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
