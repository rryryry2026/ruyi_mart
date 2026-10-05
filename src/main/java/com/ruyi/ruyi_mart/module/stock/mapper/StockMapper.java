package com.ruyi.ruyi_mart.module.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.stock.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存读写 Mapper。
 *
 * 扣减/回补一律是"把判断条件写进 WHERE、再看影响行数"的单条原子 SQL，
 * 不做"先查出来判断、再在 Java 里算好值写回"——后者是读-改-写，并发下会互相覆盖。
 * 这样既不依赖乐观锁也不依赖分布式锁：由 InnoDB 行锁保证同一行串行执行，
 * 影响 0 行就是"库存不足/已被处理"这一终态结论，调用方不需要重试。
 *
 * 五个改动库存的语句都刷新 update_time，让管理端列表里的"更新时间"是真实值。
 * （注意：WHERE 里的 locked >= n、available + n <= total 只是边界保护，
 *   保证数值不会越界，并不等于业务幂等 —— 幂等要在调用方通过状态流转来保证。）
 */
@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    /**预扣：可用转锁定。available 不足时影响 0 行，即库存不足*/
    @Update("UPDATE stock SET available = available - #{n}, locked = locked + #{n}, update_time = NOW() " +
            "WHERE product_id = #{id} AND available >= #{n}")
    int preDeduct(@Param("id") Long id,@Param("n") Integer n);

    /**确认扣减：把锁定的部分消耗掉*/
    @Update("UPDATE stock SET locked = locked - #{n}, update_time = NOW() " +
            "WHERE product_id = #{id} AND locked >= #{n}")
    int confirmDeduct(@Param("id") Long id, @Param("n") Integer n);

    /**回补：锁定转回可用（取消订单、超时关单时调用）*/
    @Update("UPDATE stock SET available = available + #{n}, locked = locked - #{n}, update_time = NOW() " +
            "WHERE product_id = #{id} AND locked >= #{n}")
    int rollback(@Param("id") Long id, @Param("n") Integer n);

    /**退款回补：货直接退回可用库存，带"不超过库存总量"的边界保护*/
    @Update("UPDATE stock SET available = available + #{n}, update_time = NOW() " +
            "WHERE product_id = #{id} AND available + #{n} <= total")
    int refundBack(@Param("id") Long id, @Param("n") Integer n);

    /**
     * 重设库存总量（可用库存按差额同步调整）
     * 单条原子 SQL 完成"读-改-写"，避免先查后改带来的并发竞态。
     * 注意赋值顺序：MySQL 的 SET 是从左到右求值，后面的表达式读到的是前面刚赋的新值，
     * 因此 available 必须排在 total 之前——否则表达式里的 total 已被覆盖，差额恒为 0。
     * WHERE 里的 #{total} >= locked 是下限保护：新总量一旦小于在途锁定量，
     * available 会被差额算成负数，而且 available + locked = total 恒等式依然成立，
     * 事后的对账校验根本查不出来——必须让这条语句本身失败（影响 0 行）。
     */
    @Update("UPDATE stock SET available = available + (#{total} - total), total = #{total}, " +
            "update_time = NOW() WHERE product_id = #{id} AND #{total} >= locked")
    int resetTotal(@Param("id") Long id, @Param("total") Integer total);
}
