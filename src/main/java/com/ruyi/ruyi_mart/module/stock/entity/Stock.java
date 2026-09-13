package com.ruyi.ruyi_mart.module.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock")
public class Stock {

    /** 主键即商品ID（一个商品一条库存记录） */
    @TableId(type = IdType.AUTO)
    private Long productId;

    private Integer total;

    private Integer available;

    private Integer locked;

    /**
     * 版本号，仅作为并发修改的痕迹字段（每次库存变更 +1）。
     * 刻意不加 @Version：本项目的库存并发控制统一由 StockMapper 中的手写原子 SQL 承担
     * （靠数据库行锁 + WHERE 条件做 CAS，如 preDeduct 的 available >= n），
     * 而非 MyBatis-Plus 乐观锁插件。加 @Version 会让 updateById 需要
     * OptimisticLockerInnerInterceptor 提供参数，未注册时会直接抛 BindingException。
     */
    private Integer version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
