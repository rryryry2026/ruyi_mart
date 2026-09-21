package com.ruyi.ruyi_mart.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.address.entity.Address;
import com.ruyi.ruyi_mart.module.address.mapper.AddressMapper;
import com.ruyi.ruyi_mart.module.cart.service.CartService;
import com.ruyi.ruyi_mart.module.cart.vo.CartItemVO;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponUseDTO;
import com.ruyi.ruyi_mart.module.coupon.service.CouponUserService;
import com.ruyi.ruyi_mart.module.order.dto.OrderAdminQueryDTO;
import com.ruyi.ruyi_mart.module.order.dto.OrderCreateDTO;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.entity.OrderItem;
import com.ruyi.ruyi_mart.module.order.enums.OrderStatus;
import com.ruyi.ruyi_mart.module.order.mapper.OrderItemMapper;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.order.mq.OrderEventProducer;
import com.ruyi.ruyi_mart.module.order.service.OrderService;
import com.ruyi.ruyi_mart.module.order.vo.OrderAdminVO;
import com.ruyi.ruyi_mart.module.order.vo.OrderVO;
import com.ruyi.ruyi_mart.module.payment.holder.PaymentStrategyHolder;
import com.ruyi.ruyi_mart.module.payment.vo.PaymentResult;
import com.ruyi.ruyi_mart.module.stock.service.StockService;
import com.ruyi.ruyi_mart.module.user.entity.User;
import com.ruyi.ruyi_mart.module.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private CartService cartService;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private PaymentStrategyHolder paymentStrategyHolder;
    @Autowired
    private StockService stockService;
    @Autowired
    private OrderEventProducer orderEventProducer;
    @Autowired
    private CouponUserService couponUserService;
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, OrderCreateDTO dto){
        List<CartItemVO> cartItems = cartService.list(userId,null);
        if(cartItems == null || cartItems.isEmpty()){
            throw new BusinessException(ResultCode.NOT_FIND,"购物车为空，无法下单");
        }

        // 收货地址：必填，且必须是本人的地址（否则可以指定别人的地址下单）
        if(dto == null || dto.getAddressId() == null){
            throw new BusinessException(ResultCode.FAIL,"请选择收货地址");
        }
        Address address = addressMapper.selectById(dto.getAddressId());
        if(address == null){
            throw new BusinessException(ResultCode.NOT_FIND,"收货地址不存在");
        }
        if(!address.getUserId().equals(userId)){
            throw new BusinessException(ResultCode.FORBIDDEN,"收货地址不属于当前用户");
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
        // 把地址内容复制进订单做快照，之后地址被改被删都不影响这一单
        order.setReceiver(address.getReceiver());
        order.setPhone(address.getPhone());
        order.setProvince(address.getProvince());
        order.setCity(address.getCity());
        order.setDistrict(address.getDistrict());
        order.setDetailAddress(address.getDetailAddress());
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

        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (dto != null && dto.getUserCouponId() != null) {
            CouponUseDTO useDTO = new CouponUseDTO();
            useDTO.setUserCouponId(dto.getUserCouponId());
            useDTO.setOrderId(order.getId());
            useDTO.setOrderAmount(totalAmount); // 用订单原价计算真实折扣
            couponDiscount = couponUserService.useCoupon(userId, useDTO);
        }
        totalAmount = totalAmount.subtract(couponDiscount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        order.setTotalAmount(totalAmount);
        baseMapper.updateById(order);

        for(OrderItem item : itemList){
            orderItemMapper.insert(item);
        }

        cartService.clearKeepStock(userId,null);
        try {
            orderEventProducer.sendCloseDelay(order.getId());
        } catch (Exception e) {
            log.error("发送关单延迟消息失败,订单{}将由定时任务兜底关闭", order.getId(), e);
        }

        // 统一走 toVO，避免这里手拼一遍字段（地址刚加进来时最容易漏）
        return toVO(order);
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
    public PaymentResult payOrder(Long userId, Long orderId, String payType) {
        Order order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FIND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该订单");
        }
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException(ResultCode.FAIL, "订单状态异常，无法支付");
        }

        // 只发起支付，返回支付载体（含支付页链接），真正的收款确认交给回调 completePayment
        PaymentResult result = paymentStrategyHolder.get(payType)
                .pay(orderId, userId, order.getTotalAmount());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completePayment(Long orderId) {
        Order order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FIND, "订单不存在");
        }
        // 幂等：已支付直接返回，避免支付平台重复回调时重复确认库存
        if (order.getStatus() == OrderStatus.PAID.getCode()) {
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException(ResultCode.FAIL, "订单状态异常，无法完成支付");
        }

        QueryWrapper<OrderItem> qw = new QueryWrapper<>();
        qw.eq("order_id", orderId);
        List<OrderItem> items = orderItemMapper.selectList(qw);
        for (OrderItem item : items) {
            stockService.confirm(item.getProductId(), item.getQuantity());
        }

        Order upd = new Order();
        upd.setId(orderId);
        upd.setStatus(OrderStatus.PAID.getCode());
        upd.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(upd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmMockPayment(Long userId, Long orderId) {
        Order order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FIND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该订单");
        }
        // 收款逻辑直接复用回调那一套，避免两处实现日后走偏
        completePayment(orderId);
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

        // 上面那句状态判断只是为了给出准确提示，真正管用的是这句条件更新。
        // 用户点"取消"的那一瞬间，定时任务可能正好在扫同一笔超时单、
        // 支付回调也可能同时到达 —— 两边都读到"待支付"就会都去回补库存，
        // 可用库存凭空多出一份（虚增 → 超卖）。
        // 抢不到这一行就说明别人已经处理了，直接报错，绝不再碰库存。
        if(baseMapper.changeStatusIf(orderId, OrderStatus.PENDING.getCode(),
                OrderStatus.CANCELLED.getCode()) == 0){
            throw new BusinessException(ResultCode.FAIL,"订单状态已变更，请刷新后重试");
        }
        releaseStock(orderId);

        return getOrderDetail(userId,orderId);
    }

    /**
     * 关闭待支付订单并回补库存。
     * 定时任务、延迟消息两个入口都走这里，保证"关单"和"回补库存"永远成对发生。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closePendingOrder(Long orderId){
        if(baseMapper.changeStatusIf(orderId, OrderStatus.PENDING.getCode(),
                OrderStatus.CLOSED.getCode()) == 0){
            log.info("订单 {} 已不是待支付状态，跳过关闭与库存回补", orderId);
            return false;
        }
        releaseStock(orderId);
        log.info("订单 {} 已关闭，回补锁定库存", orderId);
        return true;
    }

    /**
     * 回补一笔订单占用的库存。
     * 只允许在"抢到状态流转"之后调用 —— 单独调它等于重复回补。
     */
    private void releaseStock(Long orderId){
        QueryWrapper<OrderItem> qw = new QueryWrapper<>();
        qw.eq("order_id",orderId);
        List<OrderItem> items = orderItemMapper.selectList(qw);
        for(OrderItem item:items){
            stockService.release(item.getProductId(), item.getQuantity());
        }
    }

    /**============ 发货  ============*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO shipOrder(Long orderId){
        Order order = baseMapper.selectById(orderId);
        if(order == null){
            throw new BusinessException(ResultCode.NOT_FIND,"订单不存在");
        }
        if(order.getStatus() != OrderStatus.PAID.getCode()){
            throw new BusinessException(ResultCode.FAIL,"只有已支付订单才能发货");
        }
        Order upd = new Order();
        upd.setId(orderId);
        upd.setStatus(OrderStatus.SHIPPED.getCode());
        upd.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(upd);
        return toVO(baseMapper.selectById(orderId));
    }

    /**============ 确认收货  ============*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO confirmReceive(Long userId, Long orderId){
        Order order = baseMapper.selectById(orderId);
        if(order == null){
            throw new BusinessException(ResultCode.NOT_FIND,"订单不存在");
        }
        if(!order.getUserId().equals(userId)){
            throw new BusinessException(ResultCode.FORBIDDEN,"无权操作该订单");
        }
        if(order.getStatus() != OrderStatus.SHIPPED.getCode()){
            throw new BusinessException(ResultCode.FAIL,"只有已发货订单才能确认收货");
        }
        Order upd = new Order();
        upd.setId(orderId);
        upd.setStatus(OrderStatus.COMPLETED.getCode());
        upd.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(upd);
        return toVO(baseMapper.selectById(orderId));
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


    @Override
    public Page<OrderAdminVO> adminListOrders(OrderAdminQueryDTO dto){
        Page<Order> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        QueryWrapper<Order> qw = new QueryWrapper<>();
        if(StringUtils.hasText(dto.getOrderNo())){
            qw.eq("order_no", dto.getOrderNo());
        }
        if(dto.getStatus() != null){
            qw.eq("status", dto.getStatus());
        }
        if(dto.getStartTime() != null){
            qw.ge("create_time", dto.getStartTime().atStartOfDay());
        }
        if(dto.getEndTime() != null){
            qw.le("create_time", dto.getEndTime().atTime(LocalTime.MAX));
        }
        if(StringUtils.hasText(dto.getKeyword())){
            // 先按买家用户名/昵称解析出用户ID集合，再过滤订单，保证分页总数正确
            List<Long> userIds = userMapper.selectList(new QueryWrapper<User>()
                            .like("username", dto.getKeyword())
                            .or().like("nickname", dto.getKeyword()))
                    .stream().map(User::getId).collect(Collectors.toList());
            if(userIds.isEmpty()){
                return new Page<>(dto.getPageNum(), dto.getPageSize());
            }
            qw.in("user_id", userIds);
        }
        qw.orderByDesc("create_time");
        baseMapper.selectPage(page, qw);

        Page<OrderAdminVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<OrderAdminVO> vos = new ArrayList<>();
        if(!page.getRecords().isEmpty()){
            Map<Long, User> userMap = userMapper.selectBatchIds(
                            page.getRecords().stream().map(Order::getUserId).collect(Collectors.toList()))
                    .stream().collect(Collectors.toMap(User::getId, Function.identity()));
            for(Order o : page.getRecords()){
                vos.add(toAdminVO(o, userMap.get(o.getUserId())));
            }
        }
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public OrderAdminVO adminGetOrderDetail(Long orderId){
        Order order = baseMapper.selectById(orderId);
        if(order == null){
            throw new BusinessException(ResultCode.NOT_FIND, "订单不存在");
        }
        User user = userMapper.selectById(order.getUserId());
        return toAdminVO(order, user);
    }

    private OrderAdminVO toAdminVO(Order order, User user){
        OrderAdminVO vo = new OrderAdminVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        QueryWrapper<OrderItem> qw = new QueryWrapper<>();
        qw.eq("order_id", order.getId());
        vo.setItems(orderItemMapper.selectList(qw));
        if(user != null){
            vo.setBuyerUsername(user.getUsername());
            vo.setBuyerNickname(user.getNickname());
        }
        return vo;
    }

    private OrderVO toVO(Order order){
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        // 收货地址快照
        vo.setReceiver(order.getReceiver());
        vo.setPhone(order.getPhone());
        vo.setProvince(order.getProvince());
        vo.setCity(order.getCity());
        vo.setDistrict(order.getDistrict());
        vo.setDetailAddress(order.getDetailAddress());
        QueryWrapper<OrderItem> qw = new QueryWrapper<>();
        qw.eq("order_id",order.getId());
        vo.setItems(orderItemMapper.selectList(qw));
        return vo;
    }

    private String generateOrderNo(){
        return System.currentTimeMillis() + UUID.randomUUID().toString().replace("-","").substring(0,6);
    }

}
