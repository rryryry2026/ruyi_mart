package com.ruyi.ruyi_mart.module.refund.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponOrderRel;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponOrderRelMapper;
import com.ruyi.ruyi_mart.module.coupon.service.CouponUserService;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.entity.OrderItem;
import com.ruyi.ruyi_mart.module.order.enums.OrderStatus;
import com.ruyi.ruyi_mart.module.order.mapper.OrderItemMapper;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.refund.entity.Refund;
import com.ruyi.ruyi_mart.module.refund.enums.RefundStatus;
import com.ruyi.ruyi_mart.module.refund.mapper.RefundMapper;
import com.ruyi.ruyi_mart.module.refund.service.RefundService;
import com.ruyi.ruyi_mart.module.refund.vo.RefundAdminVO;
import com.ruyi.ruyi_mart.module.stock.service.StockService;
import com.ruyi.ruyi_mart.module.user.entity.User;
import com.ruyi.ruyi_mart.module.user.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RefundServiceImpl extends ServiceImpl<RefundMapper, Refund> implements RefundService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private StockService stockService;
    @Autowired
    private CouponUserService couponUserService;
    @Autowired
    private CouponOrderRelMapper couponOrderRelMapper;
    @Autowired
    private UserMapper userMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Refund apply(Long userId, Long orderId, String reason){
        // 原因必填且限长：空原因走不进审核，超长文本会把审计与列表页撑坏
        if(!StringUtils.hasText(reason)){
            throw new BusinessException(ResultCode.FAIL, "退款原因不能为空");
        }
        if(reason.length() > REASON_MAX_LENGTH){
            throw new BusinessException(ResultCode.FAIL, "退款原因不能超过" + REASON_MAX_LENGTH + "字");
        }
        // 加行锁读订单：下面的"有没有进行中的退款"是查完再插入，不串行化的话
        // 同一笔订单并发申请会插出两张退款单，之后各审各的、库存回补两次
        Order order = orderMapper.selectByIdForUpdate(orderId);
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

        LambdaQueryWrapper<Refund> qw = new LambdaQueryWrapper<>();
        qw.eq(Refund::getOrderId,orderId)
                .in(Refund::getStatus, RefundStatus.PENDING.getCode(),RefundStatus.REFUNDED.getCode());
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
        LambdaQueryWrapper<Refund> qw = new LambdaQueryWrapper<>();
        qw.eq(Refund::getUserId,userId).orderByDesc(Refund::getCreateTime);
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
        // 先抢状态流转，再动订单和库存。
        // 原来是"查出来看状态 → 退库存 → 回滚券 → 最后 updateById"，
        // 那是读-改-写：两个管理员同时点"同意"都会读到"待审核"、双双通过检查，
        // 结果库存回补两次、订单退款两次。抢不到就说明已经被审过了。
        // 优惠券那边还有一层幂等兜底（refundRollback 只认"已用→已退回"），
        // 但库存没有，必须在入口拦住。
        if(baseMapper.changeStatusIf(refundId, RefundStatus.PENDING.getCode(),
                RefundStatus.REFUNDED.getCode()) == 0){
            throw new BusinessException(ResultCode.FAIL, "只有待审核的退款单才能审核");
        }
        Long orderId = refund.getOrderId();
        LambdaQueryWrapper<OrderItem> itemQw = new LambdaQueryWrapper<>();
        itemQw.eq(OrderItem::getOrderId,orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemQw);
        for(OrderItem item : items){
            stockService.refund(item.getProductId(),item.getQuantity());
        }

        LambdaQueryWrapper<CouponOrderRel> relQw = new LambdaQueryWrapper<>();
        relQw.eq(CouponOrderRel::getOrderId, orderId).eq(CouponOrderRel::getRelStatus, 1);
        List<CouponOrderRel> rels = couponOrderRelMapper.selectList(relQw);
        for (CouponOrderRel rel : rels) {
            couponUserService.refundRollback(rel.getUserCouponId());
        }

        /**
         * 订单状态同样走条件更新，且限定"从哪些状态转过来"。
         * 原来是只带 id 的 updateById（无条件覆盖）：审核期间订单可能已被并发流程改了状态，
         * 无条件写回会把比如"已发货"直接压成"已退款"，状态与事实不符。
         * 只有订单仍处于申请时可退的三种状态才允许置为已退款，抢不到就说明状态已被别人动过。
         */
        List<Integer> refundableStatuses = List.of(
                OrderStatus.PAID.getCode(), OrderStatus.SHIPPED.getCode(), OrderStatus.COMPLETED.getCode());
        if(orderMapper.changeStatusIfIn(orderId, OrderStatus.REFUNDED.getCode(), refundableStatuses) == 0){
            throw new BusinessException(ResultCode.FAIL, "订单状态已变更，无法审核通过");
        }

        // 状态已由上面的条件更新落库，这里只同步内存对象供返回值使用
        refund.setStatus(RefundStatus.REFUNDED.getCode());
        refund.setUpdateTime(LocalDateTime.now());
        return refund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Refund reject(Long refundId, String rejectReason){
        if(!StringUtils.hasText(rejectReason)){
            throw new BusinessException(ResultCode.FAIL, "拒绝原因不能为空");
        }
        if(rejectReason.length() > REASON_MAX_LENGTH){
            throw new BusinessException(ResultCode.FAIL, "拒绝原因不能超过" + REASON_MAX_LENGTH + "字");
        }
        Refund refund = baseMapper.selectById(refundId);
        if(refund == null){
            throw new BusinessException(ResultCode.NOT_FIND, "退款单不存在");
        }
        if(refund.getStatus() != RefundStatus.PENDING.getCode()){
            throw new BusinessException(ResultCode.FAIL, "只有待审核的退款单才能审核");
        }
        /**
         * 和 approve 一样先抢状态流转，不要无条件 updateById。
         * 否则在"读到待审核"和"写回已拒绝"之间，另一个管理员点了同意
         * （CAS 成功、退了库存、回滚了券），随后这次 reject 又把单子改成"已拒绝" ——
         * 单子显示已拒绝，但钱、库存、券其实都已经退回去了，状态与事实不符。
         */
        if(baseMapper.changeStatusIf(refundId, RefundStatus.PENDING.getCode(),
                RefundStatus.REJECTED.getCode()) == 0){
            throw new BusinessException(ResultCode.FAIL, "只有待审核的退款单才能审核");
        }
        refund.setStatus(RefundStatus.REJECTED.getCode());
        refund.setRejectReason(rejectReason);
        refund.setUpdateTime(LocalDateTime.now());
        return refund;
    }

    @Override
    public Page<RefundAdminVO> adminPageRefunds(Integer status, int pageNum, int pageSize){
        // 分页参数夹在合理区间：pageSize 传超大值会把整表捞进内存
        pageNum = Math.max(pageNum, 1);
        pageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        Page<Refund> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Refund> qw = new LambdaQueryWrapper<>();
        if(status != null){
            qw.eq(Refund::getStatus, status);
        }
        qw.orderByDesc(Refund::getCreateTime);
        this.page(page, qw);

        Page<RefundAdminVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<RefundAdminVO> vos = new ArrayList<>();
        if(!page.getRecords().isEmpty()){
            Set<Long> orderIds = page.getRecords().stream()
                    .map(Refund::getOrderId).collect(Collectors.toSet());
            Map<Long, Order> orderMap = orderMapper.selectBatchIds(orderIds).stream()
                    .collect(Collectors.toMap(Order::getId, Function.identity()));
            Set<Long> userIds = page.getRecords().stream()
                    .map(Refund::getUserId).collect(Collectors.toSet());
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            for(Refund r : page.getRecords()){
                RefundAdminVO vo = new RefundAdminVO();
                BeanUtils.copyProperties(r, vo);
                Order order = orderMap.get(r.getOrderId());
                if(order != null){
                    vo.setOrderNo(order.getOrderNo());
                }
                User user = userMap.get(r.getUserId());
                if(user != null){
                    vo.setBuyerNickname(user.getNickname());
                }
                vos.add(vo);
            }
        }
        voPage.setRecords(vos);
        return voPage;
    }

    /**退款原因/拒绝原因的最大长度*/
    private static final int REASON_MAX_LENGTH = 200;

    /**分页每页条数上限*/
    private static final int MAX_PAGE_SIZE = 100;

    private String generateRefundNo() {
        return "RF" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }


}
