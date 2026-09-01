package com.ruyi.ruyi_mart.module.review.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.module.review.dto.AppendProductFirstCommentDTO;
import com.ruyi.ruyi_mart.module.review.dto.FirstProductCommentDTO;
import com.ruyi.ruyi_mart.module.review.dto.SecondProductCommentDTO;
import com.ruyi.ruyi_mart.module.review.vo.ProductAppendCommentVO;
import com.ruyi.ruyi_mart.module.review.vo.ProductFirstCommentVO;
import com.ruyi.ruyi_mart.module.review.vo.ProductSecondCommentVO;

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
}
