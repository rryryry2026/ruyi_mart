package com.ruyi.ruyi_mart.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 只有当前状态等于 from 才改成 to，返回影响行数（0 表示状态已被别人改过）。
     *
     * 订单的状态流转都该走这里，不要用"selectById 看状态 + updateById 写回"：
     * 那是读-改-写，并发下两边会同时读到"待支付"，于是双双通过检查、
     * 各自写一遍。典型场景是用户点取消的同时定时任务在关单——两条路都会去
     * 回补库存，库存就被加了两遍（虚增，比少卖更危险，直接导致超卖）。
     * 把原状态写进 WHERE 之后只有一个能成功，另一个拿到 0 行，
     * 就知道"这单已经被处理过了"，据此跳过后续的退款/回补动作。
     * 与 CouponUserMapper.changeStatusIf 同一套做法。
     */
    @Update("UPDATE order_info SET status = #{to}, update_time = NOW() " +
            "WHERE id = #{id} AND status = #{from}")
    int changeStatusIf(@Param("id") Long id,
                       @Param("from") Integer from,
                       @Param("to") Integer to);

    /**
     * 同 changeStatusIf，但允许一组合法的起始状态（IN 条件）。
     * 退款审核把订单置为"已退款"时用：申请时订单可能是已支付/已发货/已完成之一，
     * 审核是另一回事务，中间订单状态可能已被别的流程改过，
     * 必须限定"只有仍处在这几种可退状态才改成已退款"，不能无条件覆盖。
     */
    @Update("<script>UPDATE order_info SET status = #{to}, update_time = NOW() " +
            "WHERE id = #{id} AND status IN " +
            "<foreach item='s' collection='froms' open='(' separator=',' close=')'>#{s}</foreach></script>")
    int changeStatusIfIn(@Param("id") Long id,
                         @Param("to") Integer to,
                         @Param("froms") java.util.List<Integer> froms);

    /**
     * 按主键加行锁取订单（退款申请时用）。
     *
     * 退款申请是"先查有没有进行中的退款单、再插入"，这个判断本身不是原子的：
     * 同一笔订单并发点两次，两个事务都查到"没有进行中的退款"、各插一张，
     * 之后两张都能审核通过 —— 库存回补两次。申请前先锁住订单行把它们串起来。
     * 与 CouponMapper.selectByIdForUpdate 同一套做法。
     */
    @Select("SELECT * FROM order_info WHERE id = #{id} FOR UPDATE")
    Order selectByIdForUpdate(@Param("id") Long id);
}
