package com.ruyi.ruyi_mart.module.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.review.dto.AppendProductFirstCommentDTO;
import com.ruyi.ruyi_mart.module.review.dto.FirstProductCommentDTO;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCommentServiceImpl extends ServiceImpl<ProductCommentMapper, ProductComment> implements ProductCommentService {

    private final ProductCommentAppendMapper appendMapper;
    private final ProductCommentLikeMapper likeMapper;


    private static final String ANONYMOUS_NICKNAME = "匿名用户";

    // ==================== 1. 发表一级评论（首评） ====================
    @Override
    public Long saveProductFirstComment(Long userId, FirstProductCommentDTO dto){
        ProductComment comment = toFirstCommentEntity(dto,userId);
        boolean saved = this.save(comment);
        if(!saved){
            throw new BusinessException(ResultCode.ERROR, "评论保存失败，请稍后再试");
        }
        return comment.getId();
    }

    // ==================== 2. 发表二级回复 ====================
    @Override
    public Long saveProductSecondComment(Long userId, SecondProductCommentDTO dto){
        Long parentId = dto.getParentId();
        if(parentId == null || parentId <=0){
            throw new BusinessException(ResultCode.FAIL, "回复必须指定有效的一级评论ID");
        }

        ProductComment firstComment = this.getById(parentId);
        if(firstComment == null){
            throw new BusinessException(ResultCode.FAIL, "一级评论不存在，无法回复");
        }
        ProductComment reply = toSecondCommentEntity(dto,userId);
        if(firstComment.getUserId().equals(userId)){
            reply.setIsBuyer(1);
        }
        boolean saved = this.save(reply);
        if(!saved){
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
                .eq(isGood, ProductComment::getIsGoodReview, 1)
                .eq(isAppend, ProductComment::getIsAppendComment, 1)
                .orderByDesc(ProductComment::getCreateTime)
                .orderByDesc(ProductComment::getId)
                .page(new Page<>(pageNum, pageSize));

        fillLike(entityPage.getRecords(),userId);
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

        if(!firstComment.getUserId().equals(userId)){
            throw new BusinessException(ResultCode.FAIL, "只能追评自己的评论");
        }

        if(firstComment.getIsAppendComment() == 1){
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
        append.setStatus((byte) 1);

        appendMapper.insert(append);
        firstComment.setIsAppendComment(1);
        this.updateById(firstComment);

    }

    // ==================== 5. 查询某一级评论下的二级回复（分页） ====================
    @Override
    public Page<ProductSecondCommentVO> getSecondCommentPage(Long userId, Long firstCommentId, int pageNum, int pageSize){

        Page<ProductComment> entityPage = this.lambdaQuery()
                .eq(ProductComment::getParentId, firstCommentId)
                .orderByDesc(ProductComment::getCreateTime)
                .orderByDesc(ProductComment::getId)
                .page(new Page<>(pageNum, pageSize));

        fillLike(entityPage.getRecords(),userId);
        List<ProductSecondCommentVO> voList = entityPage.getRecords().stream()
                .map(this::toSecondCommentVO)
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
                        .eq(ProductCommentAppend::getCommentId,firstCommentId));
        if(append == null){
            return  null;
        }
        return toAppendCommentVO(append);
    }

    // ==================== 7. 统计某商品评论总数（一级评论数） ====================
    @Override
    public Long getProductCommentCount(Long productId){
        return this.lambdaQuery()
                .eq(ProductComment::getProductId, productId)
                .eq(ProductComment::getParentId, 0L)
                .count();
    }

    // ==================== 8. 点赞 / 取消点赞（同步落库） ====================
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

        ProductCommentLike like = likeMapper.selectOne(new LambdaQueryWrapper<ProductCommentLike>()
                .eq(ProductCommentLike::getCommentId, commentId)
                .eq(ProductCommentLike::getUserId, userId));

        int delta = 0;
        if (like == null) {
            like = new ProductCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setStatus(isLike.byteValue());
            likeMapper.insert(like);
            delta = (isLike == 1) ? 1 : 0;
        } else {
            int prev = like.getStatus();
            if (prev == isLike) {
                return; // 已处于目标状态，幂等直接返回
            }
            like.setStatus(isLike.byteValue());
            likeMapper.updateById(like);
            delta = (isLike == 1) ? 1 : -1;
        }
        // 同步主表点赞数
        if (delta != 0) {
            comment.setLikeCount(comment.getLikeCount() + delta);
            this.updateById(comment);
        }
    }

    // ==================== 私有辅助：填充当前用户点赞态 ====================
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

    // ==================== 私有辅助：DTO -> Entity 转换 ====================
    private ProductComment toFirstCommentEntity(FirstProductCommentDTO dto, Long userId) {
        ProductComment c = new ProductComment();
        c.setProductId(dto.getProductId());
        c.setProductSpecId(dto.getProductSpecId());
        c.setProductSpecText(dto.getProductSpecText());
        c.setOrderNo(dto.getOrderNo());
        c.setUserId(userId);
        c.setUserNickname(dto.getUserNickname());
        c.setUserAvatar(dto.getUserAvatar());
        c.setParentId(0L);
        c.setIsBuyer(1);                 // 原项目首评默认标记买家
        c.setIsAppendComment(0);
        c.setIsAnonymous(dto.getIsAnonymous());
        c.setRating(dto.getRating().byteValue());
        c.setContent(dto.getContent());
        c.setImageUrls(dto.getImageUrls());
        c.setLikeCount(0);
        c.setStatus((byte) 1);
        // 匿名处理：与原项目 testIsAnonymous 一致
        if (dto.getIsAnonymous() == 1) {
            c.setUserNickname(ANONYMOUS_NICKNAME);
            c.setUserAvatar("");
        }
        // 好评标记：由 rating 推导，写入确保与主表列一致
        c.setIsGoodReview(dto.getRating() >= 4 ? 1: 0);
        return c;
    }

    private ProductComment toSecondCommentEntity(SecondProductCommentDTO dto, Long userId) {
        ProductComment c = new ProductComment();
        c.setProductId(dto.getProductId());
        c.setParentId(dto.getParentId());
        c.setUserId(userId);
        c.setUserNickname(dto.getUserNickname());
        c.setUserAvatar(dto.getUserAvatar());
        c.setReplyUserId(dto.getReplyUserId());
        c.setReplyUserNickname(dto.getReplyUserNickname());
        c.setIsAnonymous(dto.getIsAnonymous());
        c.setRating((byte) 0);           // 二级回复无评分
        c.setContent(dto.getContent());
        c.setImageUrls(dto.getImageUrls());
        c.setLikeCount(0);
        c.setStatus((byte) 1);
        if (dto.getIsAnonymous() == 1) {
            c.setUserNickname(ANONYMOUS_NICKNAME);
            c.setUserAvatar("");
        }
        return c;
    }

    // ==================== 私有辅助：Entity -> VO 转换 ====================
    private ProductFirstCommentVO toFirstCommentVO(ProductComment c) {
        ProductFirstCommentVO vo = new ProductFirstCommentVO();
        vo.setId(c.getId());
        vo.setProductId(c.getProductId());
        vo.setProductSpecId(c.getProductSpecId());
        vo.setProductSpecText(c.getProductSpecText());
        vo.setUserId(c.getUserId());
        vo.setUserNickname(c.getUserNickname());
        vo.setUserAvatar(c.getUserAvatar());
        vo.setIsBuyer(c.getIsBuyer());
        vo.setIsAppendComment(c.getIsAppendComment());
        vo.setIsAnonymous(c.getIsAnonymous());
        vo.setIsGoodReview(c.getIsGoodReview());
        vo.setRating(c.getRating() == null ? 0 : c.getRating().intValue());
        vo.setContent(c.getContent());
        vo.setImageUrls(c.getImageUrls());
        vo.setLikeCount(c.getLikeCount());
        vo.setLike(c.isLike());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    private ProductSecondCommentVO toSecondCommentVO(ProductComment c) {
        ProductSecondCommentVO vo = new ProductSecondCommentVO();
        vo.setId(c.getId());
        vo.setProductId(c.getProductId());
        vo.setUserId(c.getUserId());
        vo.setUserNickname(c.getUserNickname());
        vo.setUserAvatar(c.getUserAvatar());
        vo.setIsBuyer(c.getIsBuyer());
        vo.setIsAnonymous(c.getIsAnonymous());
        vo.setContent(c.getContent());
        vo.setImageUrls(c.getImageUrls());
        vo.setLikeCount(c.getLikeCount());
        vo.setLike(c.isLike());
        vo.setReplyUserId(c.getReplyUserId());
        vo.setReplyUserNickname(c.getReplyUserNickname());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    private ProductAppendCommentVO toAppendCommentVO(ProductCommentAppend a) {
        ProductAppendCommentVO vo = new ProductAppendCommentVO();
        vo.setId(a.getId());
        vo.setUserId(a.getUserId());
        vo.setContent(a.getContent());
        vo.setImageUrls(a.getImageUrls());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }

}

