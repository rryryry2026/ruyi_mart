package com.ruyi.ruyi_mart.module.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.BeanUtils;
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
    /**用户类型：1=系统管理员。本项目里管理员即商家，其回复标"商家"*/
    private static final int USER_TYPE_ADMIN = 1;
    /**一条评论最多带几张图*/
    private static final int MAX_COMMENT_IMAGES = 9;

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
    @Autowired
    private ObjectMapper objectMapper;

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
        //能直接对应的字段一次搬完（商品、规格、订单号、内容、评分、是否匿名）
        BeanUtils.copyProperties(dto, comment);
        //图片要先校验再落库：copyProperties 搬的是原值，这里覆盖成校验后的
        comment.setImageUrls(validateImageUrls(dto.getImageUrls()));
        //以下是服务端说了算的字段，一律显式写，不给前端覆盖的机会
        comment.setUserId(userId);
        fillAuthor(comment, userId);
        comment.setParentId(0L);            //一级评论
        comment.setIsBuyer(1);              //能走到这里说明订单校验已经过了，这个标记才是真的
        comment.setIsGoodReview(dto.getRating() >= GOOD_REVIEW_MIN_RATING ? 1 : 0);
        applyNewCommentDefaults(comment);
        applyAnonymous(comment, dto.getIsAnonymous());

        try{
            if(!this.save(comment)){
                throw new BusinessException(ResultCode.ERROR, "评论保存失败，请稍后再试");
            }
        }catch (DuplicateKeyException e){
            /**
             * 表上有 uk_order_product(order_no, product_id, parent_id)：连点两下时两个请求
             * 可能都通过了上面的查重，后插的那条会撞唯一键，这里翻译成一句人话。
             *
             * 这里能安全 catch 的前提是：本方法【没有 @Transactional】。
             * 一旦将来给它加上事务，唯一键异常会先把事务标记成 rollback-only，
             * catch 住也没用，提交时照样抛 UnexpectedRollbackException（偶发 500）。
             * 到那时要么把事务去掉，要么改成"先抢占再插入"的条件更新。
             */
            throw new BusinessException(ResultCode.FAIL, "该订单的这个商品已经评价过了");
        }
        return comment.getId();
    }

    // ==================== 2. 发表二级回复 ====================

    @Override
    public Long saveProductSecondComment(Long userId, SecondProductCommentDTO dto){
        ProductComment parent = this.getById(dto.getParentId());
        if(parent == null){
            throw new BusinessException(ResultCode.NOT_FIND, "被回复的评论不存在，无法回复");
        }
        /**
         * 被隐藏的评论不能再被回复。
         * 否则会源源不断地造出"父评论看不见、回复却看得见"的孤儿内容 ——
         * 读侧看到的是旧内容，写侧却能持续产生新内容，后者更难收拾。
         */
        if(!isVisible(parent)){
            throw new BusinessException(ResultCode.FAIL, "该评论不可回复");
        }

        //本人补充说明才算买家
        Integer isBuyer = parent.getUserId().equals(userId) ? 1 : 0;
        ProductComment reply = buildSecondComment(parent, userId, isBuyer,
                dto.getIsAnonymous(), dto.getContent(), dto.getImageUrls());
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

        /**
         * 先抢"未追评 → 已追评"这个状态流转，抢到才插追评。
         * 原来是"查一下 isAppendComment、最后随手 updateById"：
         * 连点两下时两个请求都会通过检查，后插的那条会撞 product_comment_append 的
         * uk_comment 唯一键抛 500；而且整行写回还会把并发点赞改过的 like_count 覆盖回去。
         */
        boolean claimed = this.lambdaUpdate()
                .eq(ProductComment::getId, firstComment.getId())
                .eq(ProductComment::getIsAppendComment, 0)
                .set(ProductComment::getIsAppendComment, 1)
                .set(ProductComment::getUpdateTime, LocalDateTime.now())
                .update();
        if(!claimed){
            throw new BusinessException(ResultCode.FAIL, "该评论已追评，不可重复追评");
        }

        ProductCommentAppend append = new ProductCommentAppend();
        append.setCommentId(firstComment.getId());
        append.setProductId(firstComment.getProductId());
        append.setProductSpecId(firstComment.getProductSpecId());
        append.setOrderNo(firstComment.getOrderNo());
        append.setUserId(userId);
        append.setContent(dto.getContent());
        append.setImageUrls(validateImageUrls(dto.getImageUrls()));
        append.setStatus(STATUS_VISIBLE);
        append.setCreateTime(LocalDateTime.now());
        appendMapper.insert(append);
    }

    // ==================== 5. 查询某一级评论下的二级回复（分页） ====================

    @Override
    public Page<ProductSecondCommentVO> getSecondCommentPage(Long userId, Long firstCommentId, int pageNum, int pageSize){
        /**
         * 父评论不存在、或已被管理端隐藏时，它下面的回复一律不返回。
         * 回复列表是按 parentId 直查的，不校验父评论状态的话，
         * 拿着 firstCommentId 就能绕开隐藏把内容读出来。
         */
        ProductComment parent = this.getById(firstCommentId);
        if(parent == null || !isVisible(parent)){
            Page<ProductSecondCommentVO> empty = new Page<>(pageNum, pageSize, 0);
            empty.setRecords(new ArrayList<>());
            return empty;
        }

        Page<ProductComment> entityPage = this.lambdaQuery()
                .eq(ProductComment::getParentId, firstCommentId)
                //被隐藏的回复同样不展示
                .eq(ProductComment::getStatus, STATUS_VISIBLE)
                .orderByDesc(ProductComment::getCreateTime)
                .orderByDesc(ProductComment::getId)
                .page(new Page<>(pageNum, pageSize));

        fillLike(entityPage.getRecords(), userId);

        //批量判断作者是不是管理员（商家回复）：一条 IN 查询，避免逐条查库
        Set<Long> authorIds = entityPage.getRecords().stream()
                .map(ProductComment::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> sellerIds = authorIds.isEmpty() ? Collections.emptySet()
                : userMapper.selectBatchIds(authorIds).stream()
                        .filter(u -> u.getUserType() != null && u.getUserType() == USER_TYPE_ADMIN)
                        .map(User::getId)
                        .collect(Collectors.toSet());

        List<ProductSecondCommentVO> voList = entityPage.getRecords().stream()
                .map(c -> toSecondCommentVO(c, sellerIds.contains(c.getUserId())))
                .collect(Collectors.toList());

        Page<ProductSecondCommentVO> voPage = new Page<>(pageNum, pageSize, entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 6. 查询某一级评论的追评（一对一） ====================

    @Override
    public ProductAppendCommentVO getAppendComment(Long firstCommentId){
        /**
         * 追评挂在首评下：首评不存在或被隐藏时，追评也不返回。
         * 否则同样能靠 firstCommentId 直读绕开隐藏。
         */
        ProductComment firstComment = this.getById(firstCommentId);
        if(firstComment == null || !isVisible(firstComment)){
            return null;
        }

        ProductCommentAppend append = appendMapper.selectOne(
                new LambdaQueryWrapper<ProductCommentAppend>()
                        .eq(ProductCommentAppend::getCommentId, firstCommentId));
        if(append == null){
            return null;
        }
        //追评人和首评是同一个人，昵称在写入时已经按匿名规则处理过（匿名的那份就是"匿名用户"）
        return toAppendCommentVO(append);
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
            throw new BusinessException(ResultCode.NOT_FIND, "评论不存在");
        }
        //隐藏的含义是"对消费端不可见且不可再交互"，否则隐藏了也还能被点赞
        if(!isVisible(comment)){
            throw new BusinessException(ResultCode.FAIL, "该评论不可操作");
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
        //链式包装器要用它自己的 page()：当参数传给 this.page(...) 会报
        //"can not use this method for getSqlFirst"（链式包装器不支持那几个方法）
        Page<ProductComment> entityPage = query
                .orderByDesc(ProductComment::getCreateTime)
                .orderByDesc(ProductComment::getId)
                .page(new Page<>(dto.getPageNum(), dto.getPageSize()));

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
            vos.add(toReviewAdminVO(c, productNameMap.get(c.getProductId()),
                    replyCountMap.getOrDefault(c.getId(), 0L)));
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
        /**
         * 只更新状态列，不回写整个实体。
         * 实体是刚查出来的快照，里面的 like_count 可能已经被并发点赞改过，
         * 整行写回会把别人的更新覆盖回去（点赞数短暂偏少）。
         * 顺便用影响行数判断评论在不在，省掉一次查询。
         */
        boolean updated = this.lambdaUpdate()
                .eq(ProductComment::getId, commentId)
                .set(ProductComment::getStatus, status)
                .set(ProductComment::getUpdateTime, LocalDateTime.now())
                .update();
        if(!updated){
            throw new BusinessException(ResultCode.NOT_FIND, "评论不存在");
        }
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
        /**
         * 也不允许回复已隐藏的评论：回复本身是可见的，父评论却看不见，
         * 用户会看到一条没有上下文的"商家回复"。要让回复生效，先把评论恢复显示。
         */
        if(!isVisible(parent)){
            throw new BusinessException(ResultCode.FAIL, "该评论已隐藏，请先恢复显示再回复");
        }

        //商家回复：不是买家、不匿名（这两项写死 0，由 buildSecondComment 统一处理）
        ProductComment reply = buildSecondComment(parent, adminUserId, 0, 0, dto.getContent(), null);
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

    /**评论是否对消费端可见（管理端隐藏后即不可见、也不可再交互）*/
    private boolean isVisible(ProductComment comment){
        return comment.getStatus() != null && comment.getStatus() == STATUS_VISIBLE;
    }

    /**
     * 校验评论图片字段。
     * 库列是 varchar(2000)，长度先由 DTO 的 @Size 挡住；这里再确认它确实是一个
     * JSON 数组、且条数不超过上限 —— 前端拿到后会 JSON.parse，塞进来一段非 JSON 文本
     * 会让它直接抛错，属于"写坏数据"，所以在入口拦住。
     *
     * @param imageUrls JSON 数组文本，可为空
     * @return 原样返回（校验通过），便于直接 set 进实体
     */
    private String validateImageUrls(String imageUrls){
        if(!StringUtils.hasText(imageUrls)){
            return imageUrls;
        }
        JsonNode node;
        try{
            node = objectMapper.readTree(imageUrls);
        }catch (JsonProcessingException e){
            throw new BusinessException(ResultCode.FAIL, "图片格式不正确");
        }
        if(!node.isArray()){
            throw new BusinessException(ResultCode.FAIL, "图片格式不正确");
        }
        if(node.size() > MAX_COMMENT_IMAGES){
            throw new BusinessException(ResultCode.FAIL, "最多上传 " + MAX_COMMENT_IMAGES + " 张图片");
        }
        for(JsonNode item : node){
            if(!item.isTextual() || !StringUtils.hasText(item.asText())){
                throw new BusinessException(ResultCode.FAIL, "图片格式不正确");
            }
        }
        return imageUrls;
    }

    /**
     * 组装一条二级回复。
     * 用户回复和商家回复只差"评论人 / 是否买家 / 是否匿名"，其余规则完全一样，
     * 放一处免得将来加字段（比如"是否商家回复"标记）时漏改其中一边。
     * 商品、被回复人一律取自被回复的那条评论，不信前端传的。
     */
    private ProductComment buildSecondComment(ProductComment parent, Long authorId, Integer isBuyer,
                                               Integer isAnonymous, String content, String imageUrls){
        ProductComment reply = new ProductComment();
        reply.setProductId(parent.getProductId());
        reply.setParentId(parent.getId());
        reply.setUserId(authorId);
        fillAuthor(reply, authorId);
        //父评论若匿名，它存的昵称本就是"匿名用户"，这里也不会把真实身份带出来
        reply.setReplyUserId(parent.getUserId());
        reply.setReplyUserNickname(parent.getUserNickname());
        reply.setIsAnonymous(isAnonymous);
        reply.setIsBuyer(isBuyer);
        reply.setRating(0);                  //二级回复无评分
        reply.setContent(content);
        reply.setImageUrls(validateImageUrls(imageUrls));
        applyNewCommentDefaults(reply);
        applyAnonymous(reply, isAnonymous);
        return reply;
    }

    /**新建评论/回复的公共初值：对消费端可见、还没追评、零点赞，并打上创建与更新时间*/
    private void applyNewCommentDefaults(ProductComment comment){
        comment.setIsAppendComment(0);
        comment.setLikeCount(0);
        comment.setStatus(STATUS_VISIBLE);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
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

    /**管理端列表用的 VO。商品名与回复数由调用方批量查好传进来，避免逐条查库*/
    private ReviewAdminVO toReviewAdminVO(ProductComment c, String productName, Long replyCount){
        ReviewAdminVO vo = new ReviewAdminVO();
        //实体与 VO 同名字段一次搬完；VO 的字段清单就是"哪些字段能出去"的白名单
        BeanUtils.copyProperties(c, vo);
        //这两个是批量查好的关联数据，实体里没有
        vo.setProductName(productName);
        vo.setReplyCount(replyCount);
        return vo;
    }

    private ProductFirstCommentVO toFirstCommentVO(ProductComment c){
        ProductFirstCommentVO vo = new ProductFirstCommentVO();
        BeanUtils.copyProperties(c, vo);   //含点赞态 like（VO 与实体同名，一并搬过去）
        return vo;
    }

    private ProductSecondCommentVO toSecondCommentVO(ProductComment c, boolean isSeller){
        ProductSecondCommentVO vo = new ProductSecondCommentVO();
        BeanUtils.copyProperties(c, vo);
        //作者是不是管理员：实体里没有这个字段，由调用方批量查好传进来
        vo.setIsSeller(isSeller ? 1 : 0);
        return vo;
    }

    private ProductAppendCommentVO toAppendCommentVO(ProductCommentAppend a){
        ProductAppendCommentVO vo = new ProductAppendCommentVO();
        BeanUtils.copyProperties(a, vo);
        return vo;
    }
}
