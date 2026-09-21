package com.ruyi.ruyi_mart.module.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.review.entity.ProductCommentLike;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductCommentLikeMapper extends BaseMapper<ProductCommentLike> {

    /**
     * 把某人对某条评论的点赞状态落到目标值：没有记录就插入，已有就改状态。
     *
     * 不能用"先查再插，撞唯一键就 catch"：这个方法跑在 @Transactional 里，
     * 唯一键异常会先把事务标记成 rollback-only，即使 catch 住了，
     * 提交时照样抛 UnexpectedRollbackException（用户看到 500）。
     * 所以用 ON DUPLICATE KEY，让它压根不抛异常。
     * 表上的 uk_comment_user(comment_id, user_id) 是这条语句的兜底。
     */
    @Insert("INSERT INTO product_comment_like (comment_id, user_id, status, create_time, update_time) " +
            "VALUES (#{commentId}, #{userId}, #{status}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE status = VALUES(status), update_time = NOW()")
    int saveOrUpdateState(@Param("commentId") Long commentId,
                          @Param("userId") Long userId,
                          @Param("status") Integer status);
}
