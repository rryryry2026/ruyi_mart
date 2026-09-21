package com.ruyi.ruyi_mart.module.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import com.ruyi.ruyi_mart.module.order.entity.OrderItem;
import com.ruyi.ruyi_mart.module.order.enums.OrderStatus;
import com.ruyi.ruyi_mart.module.order.mapper.OrderItemMapper;
import com.ruyi.ruyi_mart.module.order.mapper.OrderMapper;
import com.ruyi.ruyi_mart.module.product.entity.Product;
import com.ruyi.ruyi_mart.module.product.mapper.ProductMapper;
import com.ruyi.ruyi_mart.module.review.dto.AdminReplyDTO;
import com.ruyi.ruyi_mart.module.review.dto.AppendProductFirstCommentDTO;
import com.ruyi.ruyi_mart.module.review.dto.FirstProductCommentDTO;
import com.ruyi.ruyi_mart.module.review.dto.ReviewAdminQueryDTO;
import com.ruyi.ruyi_mart.module.review.dto.SecondProductCommentDTO;
import com.ruyi.ruyi_mart.module.review.entity.ProductComment;
import com.ruyi.ruyi_mart.module.review.entity.ProductCommentAppend;
import com.ruyi.ruyi_mart.module.review.entity.ProductCommentLike;
import com.ruyi.ruyi_mart.module.review.mapper.ProductCommentAppendMapper;
import com.ruyi.ruyi_mart.module.review.mapper.ProductCommentLikeMapper;
import com.ruyi.ruyi_mart.module.review.mapper.ProductCommentMapper;
import com.ruyi.ruyi_mart.module.review.service.ProductCommentService;
import com.ruyi.ruyi_mart.module.review.vo.ProductAppendCommentVO;
import com.ruyi.ruyi_mart.module.review.vo.ProductFirstCommentVO;
import com.ruyi.ruyi_mart.module.review.vo.ProductSecondCommentVO;
import com.ruyi.ruyi_mart.module.review.vo.ReviewAdminVO;
import com.ruyi.ruyi_mart.module.user.entity.User;
import com.ruyi.ruyi_mart.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品评价：首评、二级回复、追评、点赞、管理端审核与商家回复。
 *
 * 有一条贯穿始终的规矩：评论人的昵称、"是否买家"、被回复人这些身份信息，
 * 一律由服务端查库或校验订单后自己填，不接受前端传入 —— 否则谁都能冒充任意身份发言。
 */
@Service
public class ProductCommentServiceImpl extends ServiceImpl<ProductCommentMapper, ProductComment> implements ProductCommentService {

    /**评论可见状态：只有 status=1 的评论对消费端展示*/
    private static final int STATUS_VISIBLE = 1;
    /**匿名评论对外展示的昵称*/
    private static final String ANONYMOUS_NICKNAME = "匿名用户";
    /**好评门槛：评分 >= 该值算好评*/
    private static final int GOOD_REVIEW_MIN_RATING = 4;

    @Autowired
    private ProductCommentAppendMapper appendMapper;
    @Autowired
    private ProductCommentLikeMapper likeMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;

    // ==================== 1. 发表一级评论（首评） ====================

    @Override
    public Long saveProductFirstComment(Long userId, FirstProductCommentDTO dto){
        //校验"确实买过"：订单是本人的、里面有这个商品、而且已完成
        assertOrderCanComment(userId, dto.getOrderNo(), dto.getProductId());
        //同一笔订单的同一个商品只允许评一次
        long commented = this.lambdaQuery()
                .eq(ProductComment::getOrderNo, dto.getOrderNo())
                .eq(ProductComment::getProductId, dto.getProductId())
                .eq(ProductComment::getParentId, 0L)
                .count();
        if(commented > 0){
            throw new BusinessException(ResultCode.FAIL, "该订单的这个商品已经评价过了");
        }

        ProductComment comment = new ProductComment();
        comment.setProductId(dto.getProductId());
        comment.setProductSpecId(dto.getProductSpecId());
        comment.setProductSpecText(dto.getProductSpecText());
        comment.setOrderNo(dto.getOrderNo());
        comment.setUserId(userId);
        fillAuthor(comment, userId);
        comment.setParentId(0L);
        //能走到这里说明订单校验已经过了，这个标记才是真的
        comment.setIsBuyer(1);
        comment.setIsAppendComment(0);
        comment.setIsAnonymous(dto.getIsAnonymous());
        comment.setRating(dto.getRating());
        comment.setIsGoodReview(dto.getRating() >= GOOD_REVIEW_MIN_RATING ? 1 : 0);
        comment.setContent(dto.getContent());
        comment.setImageUrls(dto.getImageUrls());
        comment.setLikeCount(0);
        comment.setStatus(STATUS_VISIBLE);
        applyAnonymous(comment, dto.getIsAnonymous());
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());

        try{
            if(!this.save(comment)){
                throw new BusinessException(ResultCode.ERROR, "评论保存失败，请稍后再试");
            }
        }catch (DuplicateKeyException e){
            //表上有 uk_order_product(order_no, product_id, parent_id)：连点两下时两个请求
            //可能都通过了上面的查重，后插的那条会撞唯一键，这里翻译成一句人话
            throw new BusinessException(ResultCode.FAIL, "该订单的这个商品已经评价过了");
        }
        return comment.getId();
    }

    // ==================== 2. 发表二级回复 ====================

    @Override
    public Long saveProductSecondComment(Long userId, SecondProductCommentDTO dto){
        ProductComment parent = this.getById(dto.getParentId());
        if(parent == null){
            throw new BusinessException(ResultCode.FAIL, "被回复的评论不存在，无法回复");
        }

        ProductComment reply = new ProductComment();
        //商品以被回复的那条评论为准，不用前端传的，避免挂到别的商品上
        reply.setProductId(parent.getProductId());
        reply.setParentId(parent.getId());
        reply.setUserId(userId);
        fillAuthor(reply, userId);
        // 被回复人直接取父评论里存好的那份。
        // 父评论如果是匿名的，它存的昵称本来就是"匿名用户"，这里也就不会把真实身份带出来。
        reply.setReplyUserId(parent.getUserId());
        reply.setReplyUserNickname(parent.getUserNickname());
        reply.setIsAnonymous(dto.getIsAnonymous());
        reply.setRating(0);                  //二级回复无评分
        reply.setIsBuyer(parent.getUserId().equals(userId) ? 1 : 0);  //本人补充说明才算买家
        reply.setIsAppendComment(0);
        reply.setIsGoodReview(0);
        reply.setContent(dto.getContent());
        reply.setImageUrls(dto.getImageUrls());
        reply.setLikeCount(0);
        reply.setStatus(STATUS_VISIBLE);
        applyAnonymous(reply, dto.getIsAnonymous());
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());

        if(!this.save(reply)){
            throw new BusinessException(ResultCode.ERROR, "回复保存失败，请稍后再试");
        }
        return reply.getId();
    }

    // ==================== 3. 按商品查询一级评论（分页 + 排序） ====================

    @Override
    public Page<ProductFirstCommentVO> getProductFirstCommentPage(Long userId, Long productId, Integer sortType, int pageNum, int pageSize){
        boolean isGood = sortType != null && sortType == 1;
        boolean isAppend = sortType != null && sortType == 2;

        Page<ProductComment> entityPage = this.lambdaQuery()
                .eq(ProductComment::getProductId, productId)
                .eq(ProductComment::getParentId, 0L)
                //被管理端隐藏的评论对消费端不可见
                .eq(ProductComment::getStatus, STATUS_VISIBLE)
                .eq(isGood, ProductComment::getIsGoodReview, 1)
                .eq(isAppend, ProductComment::getIsAppendComment, 1)
                .orderByDesc(ProductComment::getCreateTime)
                .orderByDesc(ProductComment::getId)
                .page(new Page<>(pageNum, pageSize));

        fillLike(entityPage.getRecords(), userId);
        List<ProductFirstCommentVO> voList = entityPage.getRecords().stream()
                .map(this::toFirstCommentVO)
                .collect(Collectors.toList());

        Page<ProductFirstCommentVO> voPage = new Page<>(pageNum, pageSize, entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 4. 对一级评论追评 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendProductFirstComment(Long userId, AppendProductFirstCommentDTO dto){
        ProductComment firstComment = this.lambdaQuery()
                .eq(ProductComment::getOrderNo, dto.getOrderNo())
                .eq(ProductComment::getUserId, userId)
                .eq(ProductComment::getProductId, dto.getProductId())
                .eq(ProductComment::getParentId, 0L)
                .one();

        if(firstComment == null){
            throw new BusinessException(ResultCode.FAIL, "未找到可追评的评论（请确认该订单已发表首评）");
        }
        if(firstComment.getIsAppendComment() != null && firstComment.getIsAppendComment() == 1){
            throw new BusinessException(ResultCode.FAIL, "该评论已追评，不可重复追评");
        }

        ProductCommentAppend append = new ProductCommentAppend();
        append.setCommentId(firstComment.getId());
        append.setProductId(firstComment.getProductId());
        append.setProductSpecId(firstComment.getProductSpecId());
        append.setOrderNo(firstComment.getOrderNo());
        append.setUserId(userId);
        append.setContent(dto.getContent());
        append.setImageUrls(dto.getImageUrls());
        append.setStatus(STATUS_VISIBLE);
        append.setCreateTime(LocalDateTime.now());
        appendMapper.insert(append);

        firstComment.setIsAppendComment(1);
        firstComment.setUpdateTime(LocalDateTime.now());
        this.updateById(firstComment);
    }

    // ==================== 5. 查询某一级评论下的二级回复（分页） ====================

    @Override
    public Page<ProductSecondCommentVO> getSecondCommentPage(Long userId, Long firstCommentId, int pageNum, int pageSize){
        Page<ProductComment> entityPage = this.lambdaQuery()
                .eq(ProductComment::getParentId, firstCommentId)
                //被隐藏的回复同样不展示
                .eq(ProductComment::getStatus, STATUS_VISIBLE)
                .orderByDesc(ProductComment::getCreateTime)
                .orderByDesc(ProductComment::getId)
                .page(new Page<>(pageNum, pageSize));

        fillLike(entityPage.getRecords(), userId);

        //父评论若是匿名的，回复里"回复 @某人"那个 userId 也不能带出去
        ProductComment parent = this.getById(firstCommentId);
        boolean maskReplyTarget = parent != null && isAnonymous(parent);

        List<ProductSecondCommentVO> voList = entityPage.getRecords().stream()
                .map(c -> toSecondCommentVO(c, maskReplyTarget))
                .collect(Collectors.toList());

        Page<ProductSecondCommentVO> voPage = new Page<>(pageNum, pageSize, entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 6. 查询某一级评论的追评（一对一） ====================

    @Override
    public ProductAppendCommentVO getAppendComment(Long firstCommentId){
        ProductCommentAppend append = appendMapper.selectOne(
                new LambdaQueryWrapper<ProductCommentAppend>()
                        .eq(ProductCommentAppend::getCommentId, firstCommentId));
        if(append == null){
            return null;
        }
        ProductAppendCommentVO vo = toAppendCommentVO(append);
        //追评人和首评是同一个人：首评匿名时，追评里同样不能带出 userId
        ProductComment firstComment = this.getById(firstCommentId);
        if(firstComment != null && isAnonymous(firstComment)){
            vo.setUserId(null);
        }
        return vo;
    }

    // ==================== 7. 统计某商品评论总数（一级评论数） ====================

    @Override
    public Long getProductCommentCount(Long productId){
        return this.lambdaQuery()
                .eq(ProductComment::getProductId, productId)
                .eq(ProductComment::getParentId, 0L)
                //计数与列表口径保持一致，隐藏的评论不计入
                .eq(ProductComment::getStatus, STATUS_VISIBLE)
                .count();
    }

    // ==================== 8. 点赞 / 取消点赞 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductCommentLike(Long userId, Long commentId, Integer isLike){
        if(isLike == null || (isLike != 0 && isLike != 1)){
            throw new BusinessException(ResultCode.FAIL, "isLike 参数必须为 0 或 1");
        }
        ProductComment comment = this.getById(commentId);
        if(comment == null){
            throw new BusinessException(ResultCode.FAIL, "评论不存在");
        }

        // 两步，都不在应用层算数：
        // 一、把点赞状态落到目标值（有记录改状态、没有就插，ON DUPLICATE KEY 不会抛异常）；
        // 二、点赞数以点赞表为准重算。
        // 这样连点两下、并发点赞、点赞与取消交错都不会把数字算歪，
        // 也不需要"我是不是新建的那条"这类说不清的判断。
        likeMapper.saveOrUpdateState(commentId, userId, isLike);
        baseMapper.refreshLikeCount(commentId);
    }

    // ==================== 9. 管理端：评价分页 ====================

    @Override
    public Page<ReviewAdminVO> adminPageComments(ReviewAdminQueryDTO dto){
        var query = this.lambdaQuery();
        //只列一级评论，二级回复在各条的详情里看
        query.eq(ProductComment::getParentId, 0L);
        if(dto.getProductId() != null){
            query.eq(ProductComment::getProductId, dto.getProductId());
        }
        if(StringUtils.hasText(dto.getKeyword())){
            query.like(ProductComment::getContent, dto.getKeyword());
        }
        if(dto.getStatus() != null){
            query.eq(ProductComment::getStatus, dto.getStatus());
        }
        if(dto.getReviewType() != null){
            query.eq(ProductComment::getIsGoodReview, dto.getReviewType());
        }
        query.orderByDesc(ProductComment::getCreateTime).orderByDesc(ProductComment::getId);

        Page<ProductComment> entityPage = this.page(new Page<>(dto.getPageNum(), dto.getPageSize()), query);

        Page<ReviewAdminVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<ProductComment> records = entityPage.getRecords();
        if(records.isEmpty()){
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        //批量取商品名，避免逐条查库
        Set<Long> productIds = records.stream()
                .map(ProductComment::getProductId).collect(Collectors.toSet());
        Map<Long, String> productNameMap = productIds.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Product::getName));

        //批量统计每条评论的二级回复数：只查 parentId 一列，回来内存分组
        List<Long> commentIds = records.stream().map(ProductComment::getId).collect(Collectors.toList());
        Map<Long, Long> replyCountMap = this.lambdaQuery()
                .select(ProductComment::getParentId)
                .in(ProductComment::getParentId, commentIds)
                .list().stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(ProductComment::getParentId, Collectors.counting()));

        List<ReviewAdminVO> vos = new ArrayList<>();
        for(ProductComment c : records){
            ReviewAdminVO vo = new ReviewAdminVO();
            vo.setId(c.getId());
            vo.setProductId(c.getProductId());
            vo.setProductName(productNameMap.get(c.getProductId()));
            vo.setUserId(c.getUserId());
            vo.setUserNickname(c.getUserNickname());
            vo.setUserAvatar(c.getUserAvatar());
            vo.setIsAnonymous(c.getIsAnonymous());
            vo.setIsBuyer(c.getIsBuyer());
            vo.setIsGoodReview(c.getIsGoodReview());
            vo.setIsAppendComment(c.getIsAppendComment());
            vo.setRating(c.getRating());
            vo.setContent(c.getContent());
            vo.setImageUrls(c.getImageUrls());
            vo.setLikeCount(c.getLikeCount());
            vo.setReplyCount(replyCountMap.getOrDefault(c.getId(), 0L));
            vo.setStatus(c.getStatus());
            vo.setCreateTime(c.getCreateTime());
            vos.add(vo);
        }
        voPage.setRecords(vos);
        return voPage;
    }

    // ==================== 10. 管理端：显示 / 隐藏 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCommentStatus(Long commentId, Integer status){
        if(status == null || (status != 0 && status != 1)){
            throw new BusinessException(ResultCode.FAIL, "status 必须为 0 或 1");
        }
        ProductComment comment = this.getById(commentId);
        if(comment == null){
            throw new BusinessException(ResultCode.NOT_FIND, "评论不存在");
        }
        comment.setStatus(status);
        comment.setUpdateTime(LocalDateTime.now());
        this.updateById(comment);
    }

    // ==================== 11. 管理端：删除评论 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId){
        ProductComment comment = this.getById(commentId);
        if(comment == null){
            throw new BusinessException(ResultCode.NOT_FIND, "评论不存在");
        }

        List<Long> ids = new ArrayList<>();
        ids.add(commentId);
        //一级评论：连带删除其下二级回复与追评记录
        if(comment.getParentId() != null && comment.getParentId() == 0L){
            List<ProductComment> children = this.lambdaQuery()
                    .eq(ProductComment::getParentId, commentId)
                    .list();
            children.forEach(child -> ids.add(child.getId()));
            appendMapper.delete(new LambdaQueryWrapper<ProductCommentAppend>()
                    .eq(ProductCommentAppend::getCommentId, commentId));
        }

        //点赞记录挂在具体评论上，一并清理，避免留下孤儿数据
        likeMapper.delete(new LambdaQueryWrapper<ProductCommentLike>()
                .in(ProductCommentLike::getCommentId, ids));
        this.removeByIds(ids);
    }

    // ==================== 12. 管理端：商家回复 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long adminReply(Long adminUserId, AdminReplyDTO dto){
        ProductComment parent = this.getById(dto.getParentId());
        if(parent == null){
            throw new BusinessException(ResultCode.NOT_FIND, "评论不存在，无法回复");
        }
        if(parent.getParentId() != null && parent.getParentId() != 0L){
            throw new BusinessException(ResultCode.FAIL, "只能回复一级评论");
        }

        ProductComment reply = new ProductComment();
        reply.setProductId(parent.getProductId());
        reply.setParentId(parent.getId());
        reply.setUserId(adminUserId);
        fillAuthor(reply, adminUserId);
        reply.setReplyUserId(parent.getUserId());
        reply.setReplyUserNickname(parent.getUserNickname());
        reply.setIsAnonymous(0);
        reply.setIsBuyer(0);
        reply.setIsAppendComment(0);
        reply.setIsGoodReview(0);
        reply.setRating(0);                  //二级回复无评分
        reply.setContent(dto.getContent());
        reply.setLikeCount(0);
        reply.setStatus(STATUS_VISIBLE);
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());

        if(!this.save(reply)){
            throw new BusinessException(ResultCode.ERROR, "回复保存失败，请稍后再试");
        }
        return reply.getId();
    }

    // ==================== 私有辅助：身份、匿名、订单校验 ====================

    /**
     * 填评论人昵称。
     * 昵称取数据库里的真实值，不接受前端传入，否则可以自称任意身份发言。
     * 头像固定留空 —— user 表没有头像字段，前端本来就会用昵称首字兜底显示，
     * 之前那个"头像"是前端把昵称首字母当字段传上来的假数据。
     */
    private void fillAuthor(ProductComment comment, Long userId){
        User author = userMapper.selectById(userId);
        if(author == null){
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录用户不存在，请重新登录");
        }
        comment.setUserNickname(StringUtils.hasText(author.getNickname()) ? author.getNickname() : author.getUsername());
        comment.setUserAvatar(null);
    }

    /**匿名评论：昵称替换成"匿名用户"，头像留空*/
    private void applyAnonymous(ProductComment comment, Integer isAnonymous){
        if(isAnonymous != null && isAnonymous == 1){
            comment.setUserNickname(ANONYMOUS_NICKNAME);
            comment.setUserAvatar(null);
        }
    }

    /**是否匿名评论*/
    private boolean isAnonymous(ProductComment comment){
        return comment.getIsAnonymous() != null && comment.getIsAnonymous() == 1;
    }

    /**
     * 校验这笔订单能不能评价：订单存在、是本人的、已完成、且里面有这个商品。
     * 界面只在"已完成"的订单上给评价入口，但接口是能直接调的，所以这里必须自己核一遍。
     */
    private void assertOrderCanComment(Long userId, String orderNo, Long productId){
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if(order == null){
            throw new BusinessException(ResultCode.NOT_FIND, "订单不存在，无法评价");
        }
        if(!order.getUserId().equals(userId)){
            throw new BusinessException(ResultCode.FORBIDDEN, "只能评价自己的订单");
        }
        if(order.getStatus() == null || order.getStatus() != OrderStatus.COMPLETED.getCode()){
            throw new BusinessException(ResultCode.FAIL, "订单完成后才能评价");
        }

        long itemCount = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .eq(OrderItem::getProductId, productId));
        if(itemCount == 0){
            throw new BusinessException(ResultCode.FAIL, "该订单里没有这个商品，无法评价");
        }
    }

    /**
     * 批量判断列表中每条评论，当前 userId 是否点过赞（1 次查询，避免 N+1）
     */
    private void fillLike(List<ProductComment> comments, Long userId){
        if(comments.isEmpty()){
            return;
        }
        if(userId == null){
            comments.forEach(c -> c.setLike(false));
            return;
        }
        List<Long> ids = comments.stream().map(ProductComment::getId).collect(Collectors.toList());
        List<ProductCommentLike> myLikes = likeMapper.selectList(new LambdaQueryWrapper<ProductCommentLike>()
                .in(ProductCommentLike::getCommentId, ids)
                .eq(ProductCommentLike::getUserId, userId)
                .eq(ProductCommentLike::getStatus, 1));
        Set<Long> likedIds = myLikes.stream().map(ProductCommentLike::getCommentId).collect(Collectors.toSet());
        comments.forEach(c -> c.setLike(likedIds.contains(c.getId())));
    }

    // ==================== 私有辅助：Entity -> VO 转换 ====================

    private ProductFirstCommentVO toFirstCommentVO(ProductComment c){
        ProductFirstCommentVO vo = new ProductFirstCommentVO();
        vo.setId(c.getId());
        vo.setProductId(c.getProductId());
        vo.setProductSpecId(c.getProductSpecId());
        vo.setProductSpecText(c.getProductSpecText());
        //匿名的评论不返回 userId，否则顺着 ID 就能反查出是谁
        vo.setUserId(isAnonymous(c) ? null : c.getUserId());
        vo.setUserNickname(c.getUserNickname());
        vo.setUserAvatar(c.getUserAvatar());
        vo.setIsBuyer(c.getIsBuyer());
        vo.setIsAppendComment(c.getIsAppendComment());
        vo.setIsAnonymous(c.getIsAnonymous());
        vo.setIsGoodReview(c.getIsGoodReview());
        vo.setRating(c.getRating() == null ? 0 : c.getRating());
        vo.setContent(c.getContent());
        vo.setImageUrls(c.getImageUrls());
        vo.setLikeCount(c.getLikeCount());
        vo.setLike(c.isLike());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    private ProductSecondCommentVO toSecondCommentVO(ProductComment c, boolean maskReplyTarget){
        ProductSecondCommentVO vo = new ProductSecondCommentVO();
        vo.setId(c.getId());
        vo.setProductId(c.getProductId());
        vo.setUserId(isAnonymous(c) ? null : c.getUserId());
        vo.setUserNickname(c.getUserNickname());
        vo.setUserAvatar(c.getUserAvatar());
        vo.setIsBuyer(c.getIsBuyer());
        vo.setIsAnonymous(c.getIsAnonymous());
        vo.setContent(c.getContent());
        vo.setImageUrls(c.getImageUrls());
        vo.setLikeCount(c.getLikeCount());
        vo.setLike(c.isLike());
        vo.setReplyUserId(maskReplyTarget ? null : c.getReplyUserId());
        vo.setReplyUserNickname(c.getReplyUserNickname());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    private ProductAppendCommentVO toAppendCommentVO(ProductCommentAppend a){
        ProductAppendCommentVO vo = new ProductAppendCommentVO();
        vo.setId(a.getId());
        vo.setUserId(a.getUserId());
        vo.setContent(a.getContent());
        vo.setImageUrls(a.getImageUrls());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }
}
