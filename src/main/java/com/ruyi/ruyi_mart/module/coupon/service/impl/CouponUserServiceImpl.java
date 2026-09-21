package com.ruyi.ruyi_mart.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponReceiveDTO;
import com.ruyi.ruyi_mart.module.coupon.dto.CouponUseDTO;
import com.ruyi.ruyi_mart.module.coupon.entity.Coupon;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponOrderRel;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponScopeDetail;
import com.ruyi.ruyi_mart.module.coupon.entity.CouponUser;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponTypeEnum;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponUseStatusEnum;
import com.ruyi.ruyi_mart.module.coupon.enums.CouponValidModeEnum;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponMapper;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponOrderRelMapper;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponScopeDetailMapper;
import com.ruyi.ruyi_mart.module.coupon.mapper.CouponUserMapper;
import com.ruyi.ruyi_mart.module.coupon.service.CouponService;
import com.ruyi.ruyi_mart.module.coupon.service.CouponUserService;
import com.ruyi.ruyi_mart.module.coupon.vo.CouponTemplateVO;
import com.ruyi.ruyi_mart.module.coupon.vo.CouponUserVO;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.entity.OrderItem;
import com.ruyi.ruyi_mart.module.order.mapper.OrderItemMapper;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.product.entity.Product;
import com.ruyi.ruyi_mart.module.product.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**用户券的实现。*/
@Service
@Slf4j
public class CouponUserServiceImpl extends ServiceImpl<CouponUserMapper, CouponUser> implements CouponUserService {

    /** 券模板状态：1 发放中（与 coupon.status 的取值一致） */
    private static final int COUPON_STATUS_RELEASING = 1;

    /** coupon_scope_detail.scope_type：1 按商品、2 按分类 */
    private static final int SCOPE_TYPE_PRODUCT = 1;

    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private CouponScopeDetailMapper couponScopeDetailMapper;
    @Autowired
    private CouponOrderRelMapper couponOrderRelMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;

    /**用户领券*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveCoupon(Long userId, CouponReceiveDTO dto){
        // 取模板必须加行锁（不能只用 getById）。
        // 下面的"单人限领"是"查已领张数 → 插一条"，本身不是原子操作：
        // 同一个用户并发点 8 次，8 个事务都读到"已领 0 张"，
        // limit_per_person=1 也能插出 8 条记录（实测 8 个并发请求全部成功）。
        // 锁住模板行之后，同一张券的领券请求排队执行，这个 count 才可信。
        // 锁粒度只到"同一张券"，不同券互不影响，也不会和下面的原子计数互相等锁。
        Coupon coupon = couponMapper.selectByIdForUpdate(dto.getCouponId());
        if(coupon == null){
            throw new BusinessException(ResultCode.NOT_FIND, "优惠券不存在");
        }
        if(coupon.getStatus() == null || coupon.getStatus() != COUPON_STATUS_RELEASING){
            throw new BusinessException(ResultCode.FAIL, "优惠券不在发放中");
        }

        long owned = count(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getCouponId, coupon.getId()));
        int limit = coupon.getLimitPerPerson() == null ? 1 : coupon.getLimitPerPerson();
        if (owned >= limit) {
            throw new BusinessException(ResultCode.FAIL, "已达到单人领取上限");
        }

        if (isBlockedByMutex(userId, coupon)) {
            throw new BusinessException(ResultCode.FAIL, "与已持有券互斥，不可同时领取");
        }

        // 先原子占用一份额度再落库。
        // 不能"先查 receiveQuota 够不够、再 +1 写回"——那是读-改-写，
        // 并发下会把最后一张券同时发给两个人（@Transactional 救不了：
        // 两个事务各自都成功了，没有半成品可回滚）。
        // 影响 0 行说明额度已被别人抢走。
        if (couponMapper.occupyReceiveQuota(coupon.getId()) == 0) {
            throw new BusinessException(ResultCode.FAIL, "优惠券已领完");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validStart = now;
        LocalDateTime validEnd;
        if (coupon.getValidMode() == CouponValidModeEnum.FIXED_TIME) {
            validStart = coupon.getValidStart();
            validEnd = coupon.getValidEnd();
        } else {
            int days = coupon.getReceiveValidDays() == null ? 0 : coupon.getReceiveValidDays();
            validEnd = now.plusDays(days);
        }
        CouponUser userCoupon = new CouponUser();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setValidStart(validStart);
        userCoupon.setValidEnd(validEnd);
        userCoupon.setUseStatus(CouponUseStatusEnum.UNUSED);
        userCoupon.setCreateTime(now);
        userCoupon.setUpdateTime(now);
        save(userCoupon);
    }

    /**
     * 是否被互斥组挡住。
     * 领券时的校验和领券中心的预筛都调它，避免同一套规则写两份、
     * 将来改了一处忘了另一处（就会出现"列表里显示能领、点下去报错"）。
     */
    private boolean isBlockedByMutex(Long userId, Coupon coupon){
        Long groupCode = coupon.getMutexGroupCode();
        if (groupCode == null || groupCode == 0) {
            return false;
        }
        List<CouponUser> mine = list(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId));
        if (mine.isEmpty()) {
            return false;
        }
        // 批量取模板（loadCouponMap 就是为了避免逐条查）
        Map<Long, Coupon> couponMap = loadCouponMap(mine);
        for (CouponUser cu : mine) {
            Coupon held = couponMap.get(cu.getCouponId());
            if (held != null && groupCode.equals(held.getMutexGroupCode())) {
                return true;
            }
        }
        return false;
    }

    /**我的券包。*/
    @Override
    public IPage<CouponUserVO> myCoupons(Long userId, Integer useStatus, Integer page, Integer size) {
        Page<CouponUser> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        IPage<CouponUser> entityPage = page(p, Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(useStatus != null, CouponUser::getUseStatus, useStatus)
                .orderByDesc(CouponUser::getCreateTime));

        // 批量取券模板，避免逐条查（N+1）
        Map<Long, Coupon> couponMap = loadCouponMap(entityPage.getRecords());
        List<CouponUserVO> voList = entityPage.getRecords().stream()
                .map(cu -> toUserVO(cu, couponMap.get(cu.getCouponId()), null))
                .collect(Collectors.toList());

        Page<CouponUserVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**结算可用券。*/
    @Override
    public List<CouponUserVO> listAvailable(Long userId, BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponUser> mine = list(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getUseStatus, CouponUseStatusEnum.UNUSED)
                .le(CouponUser::getValidStart, now)
                .ge(CouponUser::getValidEnd, now));

        Map<Long, Coupon> couponMap = loadCouponMap(mine);
        List<CouponUserVO> result = new ArrayList<>();
        for (CouponUser cu : mine) {
            Coupon coupon = couponMap.get(cu.getCouponId());
            BigDecimal discount = null;
            if (orderAmount != null && coupon != null) {
                discount = calcDiscount(coupon, orderAmount);
                // 传了订单金额，就把不满足门槛（抵扣为 0）的券滤掉，
                // 免得结算页列出一堆用不了的券
                if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
            }
            result.add(toUserVO(cu, coupon, discount));
        }
        return result;
    }

    /**领券中心。*/
    @Override
    public List<CouponTemplateVO> listReceivable(Long userId) {
        // 发放中、且未被隐藏的券模板
        List<Coupon> templates = couponService.list(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getStatus, 1)
                .eq(Coupon::getIsElimination, 0)
                .orderByDesc(Coupon::getCreateTime));
        if (templates.isEmpty()) {
            return new ArrayList<>();
        }

        // 当前用户已持有的券，用于算"已领张数"
        List<CouponUser> mine = list(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId));
        Map<Long, Long> ownedCountMap = mine.stream()
                .collect(Collectors.groupingBy(CouponUser::getCouponId, Collectors.counting()));

        List<CouponTemplateVO> result = new ArrayList<>();
        for (Coupon coupon : templates) {
            int total = coupon.getTotalQuota() == null ? 0 : coupon.getTotalQuota();
            int received = coupon.getReceiveQuota() == null ? 0 : coupon.getReceiveQuota();
            Integer remainQuota = total > 0 ? Math.max(0, total - received) : null;
            if (remainQuota != null && remainQuota <= 0) {
                continue; // 已领完
            }

            long owned = ownedCountMap.getOrDefault(coupon.getId(), 0L);
            int limit = coupon.getLimitPerPerson() == null ? 1 : coupon.getLimitPerPerson();

            // 互斥判断复用领券时的同一段逻辑，避免规则写两份
            boolean blockedByMutex = isBlockedByMutex(userId, coupon);

            int remainToReceive = blockedByMutex ? 0 : (int) Math.max(0, limit - owned);
            if (remainToReceive <= 0) {
                continue; // 已达单人上限 / 被互斥挡住
            }

            CouponTemplateVO vo = new CouponTemplateVO();
            vo.setId(coupon.getId());
            vo.setCouponNo(coupon.getCouponNo());
            vo.setActivityName(coupon.getActivityName());
            vo.setCouponType(coupon.getCouponType());
            vo.setFaceValue(coupon.getFaceValue());
            vo.setDiscountRate(coupon.getDiscountRate());
            vo.setMaxDiscount(coupon.getMaxDiscount());
            vo.setMinSpend(coupon.getMinSpend());
            vo.setValidMode(coupon.getValidMode());
            vo.setValidStart(coupon.getValidStart());
            vo.setValidEnd(coupon.getValidEnd());
            vo.setReceiveValidDays(coupon.getReceiveValidDays());
            vo.setLimitPerPerson(limit);
            vo.setReceiveQuota(received);
            vo.setTotalQuota(total);
            vo.setUseScope(coupon.getUseScope());
            vo.setOwnedCount((int) owned);
            vo.setRemainToReceive(remainToReceive);
            vo.setRemainQuota(remainQuota);
            result.add(vo);
        }
        return result;
    }

    /** 批量取券模板，避免逐条查询 */
    private Map<Long, Coupon> loadCouponMap(List<CouponUser> userCoupons) {
        if (userCoupons == null || userCoupons.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = userCoupons.stream()
                .map(CouponUser::getCouponId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return couponService.listByIds(ids).stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c, (a, b) -> a));
    }

    /**拼vo对象。*/
    private CouponUserVO toUserVO(CouponUser cu, Coupon coupon, BigDecimal discountAmount) {
        CouponUserVO vo = new CouponUserVO();
        vo.setId(cu.getId());
        vo.setCouponId(cu.getCouponId());
        vo.setUseStatus(cu.getUseStatus());
        vo.setValidStart(cu.getValidStart());
        vo.setValidEnd(cu.getValidEnd());
        vo.setCreateTime(cu.getCreateTime());
        vo.setDiscountAmount(discountAmount);
        if (coupon != null) {
            vo.setActivityName(coupon.getActivityName());
            vo.setCouponType(coupon.getCouponType());
            vo.setFaceValue(coupon.getFaceValue());
            vo.setDiscountRate(coupon.getDiscountRate());
            vo.setMaxDiscount(coupon.getMaxDiscount());
            vo.setMinSpend(coupon.getMinSpend());
            vo.setUseScope(coupon.getUseScope());
        }
        return vo;
    }

    /**核销券。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal useCoupon(Long userId, CouponUseDTO dto) {
        CouponUser userCoupon = getById(dto.getUserCouponId());
        if (userCoupon == null) {
            throw new BusinessException(ResultCode.NOT_FIND, "用户券不存在");
        }
        if (!userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权使用该券");
        }
        if (userCoupon.getUseStatus() != CouponUseStatusEnum.UNUSED) {
            throw new BusinessException(ResultCode.FAIL, "该券不可使用");
        }

        // orderAmount 必填。
        // 缺了它就没法判断满减券的门槛——calcDiscount 会对 null 走"按面值兜底"，
        // 等于"满800减100"直接减100，门槛形同虚设。
        if (dto.getOrderAmount() == null) {
            throw new BusinessException(ResultCode.FAIL, "缺少订单金额，无法计算优惠");
        }

        // 订单必须存在且属于本人，否则会往别人的订单上写券关联（退款时会连带回滚）
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FIND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权在该订单上使用优惠券");
        }

        LocalDateTime now = LocalDateTime.now();
        // 有效期字段缺失的券直接拒用：SQL 那边 valid_start <= now 也查不出它，
        // 两边判断保持一致，避免"列表里看不到、构造请求却能用"
        if (userCoupon.getValidStart() == null || userCoupon.getValidEnd() == null) {
            throw new BusinessException(ResultCode.FAIL, "该券有效期异常，无法使用");
        }
        if (now.isBefore(userCoupon.getValidStart())) {
            throw new BusinessException(ResultCode.FAIL, "该券未到生效时间");
        }
        if (now.isAfter(userCoupon.getValidEnd())) {
            throw new BusinessException(ResultCode.FAIL, "该券已过期");
        }
        Coupon coupon = couponService.getById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FIND, "优惠券模板不存在");
        }
        if (!isInScope(coupon, dto, order)) {
            throw new BusinessException(ResultCode.FAIL, "该券不适用于本单商品");
        }
        BigDecimal discount = calcDiscount(coupon, dto.getOrderAmount());
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.FAIL, "该优惠券不满足使用条件或抵扣金额为0");
        }

        // 条件更新：只有仍处于"未用"才能改成"已用"。
        // 原先是无条件 updateById，用户手抖双击或并发请求会让同一张券被核销两次
        // （写两条 coupon_order_rel、抵扣两次），事务挡不住这种情况。
        if (baseMapper.changeStatusIf(userCoupon.getId(),
                CouponUseStatusEnum.UNUSED.getCode(),
                CouponUseStatusEnum.USED.getCode()) == 0) {
            throw new BusinessException(ResultCode.FAIL, "该券已被使用");
        }

        CouponOrderRel rel = new CouponOrderRel();
        rel.setOrderId(dto.getOrderId());
        rel.setOrderItemId(dto.getOrderItemId() == null ? 0L : dto.getOrderItemId());
        rel.setUserCouponId(userCoupon.getId());
        rel.setDiscountAmount(discount);
        rel.setRelStatus(1);
        rel.setUseTime(now);
        rel.setCreateTime(now);
        couponOrderRelMapper.insert(rel);

        // 计数走原子 SQL，不整行 updateById（否则会覆盖别人并发改的其它列）
        couponMapper.increaseUsedQuota(coupon.getId());
        return discount;
    }

    /**
     * 判断券的适用范围。
     * use_scope：1 全场（直接通过）、2 指定商品、3 指定分类。
     * 原先这个字段完全没生效——单品券、分类券和全场券行为一样，在任意商品上都能减。
     */
    private boolean isInScope(Coupon coupon, CouponUseDTO dto, Order order){
        Integer useScope = coupon.getUseScope();
        if (useScope == null || useScope == 1) {
            return true;
        }

        List<CouponScopeDetail> scopes = couponScopeDetailMapper.selectList(
                Wrappers.<CouponScopeDetail>lambdaQuery().eq(CouponScopeDetail::getCouponId, coupon.getId()));
        if (scopes.isEmpty()) {
            // 配了范围却没配明细，视为不可用，避免"范围券变成全场券"
            return false;
        }

        // 单品券：只认调用方指定的那一项订单明细
        if (dto.getOrderItemId() != null && dto.getOrderItemId() != 0) {
            OrderItem item = orderItemMapper.selectById(dto.getOrderItemId());
            if (item != null && item.getOrderId().equals(order.getId())) {
                return scopeHits(scopes, item.getProductId());
            }
        }
        // 未指定明细（或明细不属于本单）：只要本单里有商品命中范围即可
        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            if (scopeHits(scopes, item.getProductId())) {
                return true;
            }
        }
        return false;
    }

    /** 商品是否落在券的适用范围内：直接命中商品，或命中商品所属分类 */
    private boolean scopeHits(List<CouponScopeDetail> scopes, Long productId){
        if (productId == null) {
            return false;
        }
        Long categoryId = null;
        Product product = productMapper.selectById(productId);
        if (product != null) {
            categoryId = product.getCategoryId();
        }
        for (CouponScopeDetail scope : scopes) {
            boolean hitProduct = scope.getScopeType() != null
                    && scope.getScopeType() == SCOPE_TYPE_PRODUCT
                    && productId.equals(scope.getTargetId());
            boolean hitCategory = scope.getScopeType() != null
                    && scope.getScopeType() != SCOPE_TYPE_PRODUCT
                    && categoryId != null && categoryId.equals(scope.getTargetId());
            if (hitProduct || hitCategory) {
                return true;
            }
        }
        return false;
    }

    /**退款回滚。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundRollback(Long userCouponId) {
        CouponUser userCoupon = getById(userCouponId);
        if (userCoupon == null) {
            return;
        }

        // 只有"已用"的券才回滚，靠条件更新的影响行数判定。
        // 原先是无条件置成"已退回"，退款流程若被重复调用，
        // used_quota 会被多减一次（Math.max 只兜住下限，兜不住重复减）。
        int changed = baseMapper.changeStatusIf(userCouponId,
                CouponUseStatusEnum.USED.getCode(),
                CouponUseStatusEnum.RETURNED.getCode());
        if (changed == 0) {
            log.info("用户券{}当前不是已用状态，跳过退款回滚", userCouponId);
            return;
        }

        // 计数走原子 SQL，不会整行覆盖、也不会减成负数
        couponMapper.decreaseUsedQuota(userCoupon.getCouponId());

        List<CouponOrderRel> rels = couponOrderRelMapper.selectList(Wrappers.<CouponOrderRel>lambdaQuery()
                .eq(CouponOrderRel::getUserCouponId, userCouponId)
                .eq(CouponOrderRel::getRelStatus, 1));
        for (CouponOrderRel rel : rels) {
            rel.setRelStatus(2);
            rel.setRefundTime(LocalDateTime.now());
            couponOrderRelMapper.updateById(rel);
        }
    }

    /**定期扫描。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<CouponUser> expired = list(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUseStatus, CouponUseStatusEnum.UNUSED)
                .lt(CouponUser::getValidEnd, now));
        for (CouponUser cu : expired) {
            cu.setUseStatus(CouponUseStatusEnum.EXPIRED);
            cu.setUpdateTime(now);
        }
        if (!expired.isEmpty()) {
            updateBatchById(expired);
            log.info("优惠券过期扫描：处理 {} 张", expired.size());
        }
    }

    /**算钱。*/
    private BigDecimal calcDiscount(Coupon coupon, BigDecimal orderAmount) {
        CouponTypeEnum type = coupon.getCouponType();

        if (orderAmount == null) {
            if (type == CouponTypeEnum.DISCOUNT) {
                return coupon.getMaxDiscount() == null ? BigDecimal.ZERO : coupon.getMaxDiscount();
            }
            return coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
        }

        if (type == CouponTypeEnum.NO_THRESHOLD) {
            return coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
        }

        if (type == CouponTypeEnum.FULL_REDUCTION) {
            BigDecimal minSpend = coupon.getMinSpend() == null ? BigDecimal.ZERO : coupon.getMinSpend();
            if (orderAmount.compareTo(minSpend) < 0) {
                return BigDecimal.ZERO;
            }
            return coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
        }

        if (type == CouponTypeEnum.DISCOUNT) {
            BigDecimal rate = coupon.getDiscountRate() == null ? BigDecimal.TEN : coupon.getDiscountRate();
            BigDecimal percent = rate.divide(BigDecimal.TEN, 4, java.math.RoundingMode.HALF_UP);
            BigDecimal discount = orderAmount.multiply(BigDecimal.ONE.subtract(percent));
            discount = discount.setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal maxDiscount = coupon.getMaxDiscount() == null ? BigDecimal.ZERO : coupon.getMaxDiscount();
            if (maxDiscount.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(maxDiscount) > 0) {
                discount = maxDiscount;
            }
            if (discount.compareTo(orderAmount) > 0) {
                discount = orderAmount;
            }
            return discount;
        }

        return coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
    }


}
