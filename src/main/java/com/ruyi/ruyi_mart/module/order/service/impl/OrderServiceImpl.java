package com.ruyi.ruyi_mart.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.ruyi.ruyi_mart.module.product.entity.Product;
import com.ruyi.ruyi_mart.module.product.mapper.ProductMapper;
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
    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, OrderCreateDTO dto){
        List<CartItemVO> cartItems = cartService.list(userId,null);
        if(cartItems == null || cartItems.isEmpty()){
            throw new BusinessException(ResultCode.NOT_FIND,"购物车为空，无法下单");
        }

        /**
         * 下单前用当前价复核一遍购物车里的快照价。
         * 购物车存的是"加购那一刻"的价格，商品涨价后老购物车仍能按旧价下单（商家吃亏），
         * 降价则用户吃亏。发现不一致就拦下来，让用户回购物车刷新后再下单 —— 电商的常规做法。
         */
        for(CartItemVO ci : cartItems){
            Product current = productMapper.selectById(ci.getProductId());
            if(current == null){
                throw new BusinessException(ResultCode.NOT_FIND, "商品已下架：" + ci.getName());
            }
            if(current.getPrice() == null || current.getPrice().compareTo(ci.getPrice()) != 0){
                throw new BusinessException(ResultCode.FAIL,
                        "商品价格已变动，请返回购物车刷新后重新下单：" + ci.getName());
            }
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
        order.setStatus(OrderStatus.PENDING.getCode());
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
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        qw.eq(Order::getUserId,userId).orderByDesc(Order::getCreateTime);
        List<Order> orders = baseMapper.selectList(qw);
        //批量转 VO：明细一次查完，避免逐单查明细的 N+1
        return toVOList(orders);
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
        /**
         * 上面那句状态判断只用来给出准确提示，真正管用的是这句条件更新。
         * 支付回调进来的同时，定时任务可能正好在关这笔超时单、用户也可能刚点了取消：
         * 两边都读到"待支付"就会各自往下走 —— 已关闭的订单被改成"已支付"，
         * 库存还被"确认"了一次（锁定转消耗）。抢不到就说明别人已经处理过了。
         * 库存确认必须放在抢到状态之后，否则输的那一方也会动库存。
         */
        if (baseMapper.changeStatusIf(orderId, OrderStatus.PENDING.getCode(),
                OrderStatus.PAID.getCode()) == 0) {
            throw new BusinessException(ResultCode.FAIL, "订单状态已变更，无法完成支付");
        }

        LambdaQueryWrapper<OrderItem> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(qw);
        for (OrderItem item : items) {
            stockService.confirm(item.getProductId(), item.getQuantity());
        }
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
        LambdaQueryWrapper<OrderItem> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderItem::getOrderId,orderId);
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
        //条件更新抢状态：退款审核可能正在把订单改成"已退款"，不能把已退款的单又改成"已发货"
        if(baseMapper.changeStatusIf(orderId, OrderStatus.PAID.getCode(),
                OrderStatus.SHIPPED.getCode()) == 0){
            throw new BusinessException(ResultCode.FAIL,"订单状态已变更，无法发货");
        }
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
        //同一笔订单可能被连点两下"确认收货"，用条件更新保证只有一次状态流转生效
        if(baseMapper.changeStatusIf(orderId, OrderStatus.SHIPPED.getCode(),
                OrderStatus.COMPLETED.getCode()) == 0){
            throw new BusinessException(ResultCode.FAIL,"订单状态已变更，无法确认收货");
        }
        return toVO(baseMapper.selectById(orderId));
    }


    @Override
    public List<OrderVO> listOrdersByStatus(Long userId, Integer status){
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        qw.eq(Order::getUserId,userId).eq(Order::getStatus,status).orderByDesc(Order::getCreateTime);
        List<Order> orders = baseMapper.selectList(qw);
        return toVOList(orders);
    }


    @Override
    public Page<OrderVO> listOrdersPage(Long userId,Integer status,int pageNum,int pageSize){
        Page<Order> page = new Page<>(pageNum,pageSize);
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        qw.eq(Order::getUserId,userId);
        if(status != null){
            qw.eq(Order::getStatus,status);
        }
        qw.orderByDesc(Order::getCreateTime);
        baseMapper.selectPage(page,qw);

        Page<OrderVO> voPage = new Page<>(page.getCurrent(),page.getSize(),page.getTotal());
        //批量转 VO：明细一次查完，避免逐单查明细的 N+1
        voPage.setRecords(toVOList(page.getRecords()));
        return voPage;
    }


    @Override
    public Page<OrderAdminVO> adminListOrders(OrderAdminQueryDTO dto){
        Page<Order> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        if(StringUtils.hasText(dto.getOrderNo())){
            qw.eq(Order::getOrderNo, dto.getOrderNo());
        }
        if(dto.getStatus() != null){
            qw.eq(Order::getStatus, dto.getStatus());
        }
        if(dto.getStartTime() != null){
            qw.ge(Order::getCreateTime, dto.getStartTime().atStartOfDay());
        }
        if(dto.getEndTime() != null){
            qw.le(Order::getCreateTime, dto.getEndTime().atTime(LocalTime.MAX));
        }
        if(StringUtils.hasText(dto.getKeyword())){
            // 先按买家用户名/昵称解析出用户ID集合，再过滤订单，保证分页总数正确
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getUsername, dto.getKeyword())
                            .or().like(User::getNickname, dto.getKeyword()))
                    .stream().map(User::getId).collect(Collectors.toList());
            if(userIds.isEmpty()){
                return new Page<>(dto.getPageNum(), dto.getPageSize());
            }
            qw.in(Order::getUserId, userIds);
        }
        qw.orderByDesc(Order::getCreateTime);
        baseMapper.selectPage(page, qw);

        Page<OrderAdminVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        //买家与明细都批量查一次，避免逐单查明细的 N+1
        voPage.setRecords(toAdminVOList(page.getRecords()));
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

    /**单笔转换：自己查一次明细*/
    private OrderAdminVO toAdminVO(Order order, User user){
        LambdaQueryWrapper<OrderItem> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderItem::getOrderId, order.getId());
        return toAdminVO(order, user, orderItemMapper.selectList(qw));
    }

    /**带明细的管理端转换：明细由 toAdminVOList 一次查好传进来*/
    private OrderAdminVO toAdminVO(Order order, User user, List<OrderItem> items){
        OrderAdminVO vo = new OrderAdminVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items);
        if(user != null){
            vo.setBuyerUsername(user.getUsername());
            vo.setBuyerNickname(user.getNickname());
        }
        return vo;
    }

    /**管理端批量转换：买家与明细各查一次，避免逐单查明细的 N+1*/
    private List<OrderAdminVO> toAdminVOList(List<Order> orders){
        if(orders.isEmpty()){
            return new ArrayList<>();
        }
        List<Long> orderIds = new ArrayList<>();
        for(Order o : orders){
            orderIds.add(o.getId());
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(
                        orders.stream().map(Order::getUserId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        LambdaQueryWrapper<OrderItem> itemQw = new LambdaQueryWrapper<>();
        itemQw.in(OrderItem::getOrderId, orderIds);
        Map<Long, List<OrderItem>> itemMap = orderItemMapper.selectList(itemQw).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<OrderAdminVO> result = new ArrayList<>();
        for(Order o : orders){
            result.add(toAdminVO(o, userMap.get(o.getUserId()),
                    itemMap.getOrDefault(o.getId(), new ArrayList<>())));
        }
        return result;
    }

    /**单笔转换：自己查一次明细*/
    private OrderVO toVO(Order order){
        LambdaQueryWrapper<OrderItem> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderItem::getOrderId, order.getId());
        return toVO(order, orderItemMapper.selectList(qw));
    }

    /**
     * 带明细的转换。
     * 列表场景要先用 toVOList 把明细一次查回来传进来，
     * 否则每笔订单都单独查一遍明细 —— 100 单就是 101 次查询。
     */
    private OrderVO toVO(Order order, List<OrderItem> items){
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
        vo.setItems(items);
        return vo;
    }

    /**批量转换：明细一次查完按 orderId 分组，避免 N+1*/
    private List<OrderVO> toVOList(List<Order> orders){
        if(orders.isEmpty()){
            return new ArrayList<>();
        }
        List<Long> orderIds = new ArrayList<>();
        for(Order o : orders){
            orderIds.add(o.getId());
        }
        LambdaQueryWrapper<OrderItem> itemQw = new LambdaQueryWrapper<>();
        itemQw.in(OrderItem::getOrderId, orderIds);
        Map<Long, List<OrderItem>> itemMap = orderItemMapper.selectList(itemQw).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<OrderVO> result = new ArrayList<>();
        for(Order o : orders){
            result.add(toVO(o, itemMap.getOrDefault(o.getId(), new ArrayList<>())));
        }
        return result;
    }

    private String generateOrderNo(){
        return System.currentTimeMillis() + UUID.randomUUID().toString().replace("-","").substring(0,6);
    }

}
