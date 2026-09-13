package com.ruyi.ruyi_mart.module.review.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.module.review.dto.AdminReplyDTO;
import com.ruyi.ruyi_mart.module.review.dto.AppendProductFirstCommentDTO;
import com.ruyi.ruyi_mart.module.review.dto.FirstProductCommentDTO;
import com.ruyi.ruyi_mart.module.review.dto.ReviewAdminQueryDTO;
import com.ruyi.ruyi_mart.module.review.dto.SecondProductCommentDTO;
import com.ruyi.ruyi_mart.module.review.vo.ProductAppendCommentVO;
import com.ruyi.ruyi_mart.module.review.vo.ProductFirstCommentVO;
import com.ruyi.ruyi_mart.module.review.vo.ProductSecondCommentVO;
import com.ruyi.ruyi_mart.module.review.vo.ReviewAdminVO;

public interface ProductCommentService {

    /**
     * 发表一级评论（首评）
     * @param userId   当前登录用户ID（由 Controller 从 SecurityContext 取，不信任前端）
     * @param dto      首评请求参数
     * @return 新评论ID
     */
    Long saveProductFirstComment(Long userId, FirstProductCommentDTO dto);

    /**
     * 发表二级回复（对某条一级评论回复）
     * @param userId   当前登录用户ID
     * @param dto      二级回复请求参数
     * @return 新回复ID
     */
    Long saveProductSecondComment(Long userId, SecondProductCommentDTO dto);

    /**
     * 按商品查询一级评论（分页 + 排序）
     * @param userId   当前登录用户ID（用于填充每条评论的 like 标记）
     * @param productId 商品ID
     * @param sortType 排序类型：0=默认(全部) 1=好评 2=追评
     * @param pageNum  页码（从1开始）
     * @param pageSize 每页条数
     * @return 一级评论分页
     */
    Page<ProductFirstCommentVO> getProductFirstCommentPage(Long userId, Long productId, Integer sortType, int pageNum, int pageSize);

    /**
     * 对一级评论追评
     * @param userId   当前登录用户ID（必须是原评论作者本人）
     * @param dto      追评请求参数（靠 orderNo 定位原评论）
     */
    void appendProductFirstComment(Long userId, AppendProductFirstCommentDTO dto);

    /**
     * 查询某条一级评论下的二级回复（分页）
     * @param userId      当前登录用户ID（填充 like 标记）
     * @param firstCommentId 一级评论ID
     * @param pageNum     页码
     * @param pageSize    每页条数
     * @return 二级回复分页
     */
    Page<ProductSecondCommentVO> getSecondCommentPage(Long userId, Long firstCommentId, int pageNum, int pageSize);

    /**
     * 查询某条一级评论的追评（一对一）
     * @param firstCommentId 一级评论ID
     * @return 追评视图（无则返回 null）
     */
    ProductAppendCommentVO getAppendComment(Long firstCommentId);

    /**
     * 统计某商品的评论总数
     * @param productId 商品ID
     * @return 评论总数（一级评论数）
     */
    Long getProductCommentCount(Long productId);

    /**
     * 点赞 / 取消点赞
     * @param userId     当前登录用户ID
     * @param commentId  评论ID（一级或二级均可，同为 product_comment 表的行）
     * @param isLike     1=点赞 0=取消点赞
     */
    void updateProductCommentLike(Long userId, Long commentId, Integer isLike);

    /**
     * 管理端：评价分页（含已隐藏的评论，可按商品/内容/状态/好评差评筛选）
     * @param dto 查询条件
     * @return 一级评论分页（含商品名与回复数）
     */
    Page<ReviewAdminVO> adminPageComments(ReviewAdminQueryDTO dto);

    /**
     * 管理端：显示/隐藏评论（隐藏后消费端查询不到）
     * @param commentId 评论ID
     * @param status    1=显示 0=隐藏
     */
    void updateCommentStatus(Long commentId, Integer status);

    /**
     * 管理端：删除评论（一级评论连带删除其二级回复、追评与点赞记录）
     * @param commentId 评论ID
     */
    void deleteComment(Long commentId);

    /**
     * 管理端：商家回复
     * @param adminUserId 当前管理员用户ID（昵称从数据库取，不信任前端）
     * @param dto         回复内容与被回复的一级评论ID
     * @return 新回复ID
     */
    Long adminReply(Long adminUserId, AdminReplyDTO dto);
}
