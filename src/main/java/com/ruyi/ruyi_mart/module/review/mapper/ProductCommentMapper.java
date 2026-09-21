package com.ruyi.ruyi_mart.module.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.review.entity.ProductComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductCommentMapper extends BaseMapper<ProductComment> {

    /**
     * 按点赞表重算点赞数，返回影响行数。
     *
     * 不自己算 delta（"查出来 +1/-1 再写回"是读-改-写，并发下会丢更新），
     * 也不让应用层算：并发插入时"我到底是不是新建的那条"本身就说不清，
     * 一旦判断错，计数就永久偏了。
     * 这里直接以点赞表为准重算 —— 单条 SQL 原子完成，不会漂移，也不会减成负数。
     */
    @Update("UPDATE product_comment SET like_count = (SELECT COUNT(*) FROM product_comment_like l " +
            "WHERE l.comment_id = product_comment.id AND l.status = 1), update_time = NOW() " +
            "WHERE id = #{id}")
    int refreshLikeCount(@Param("id") Long id);
}
