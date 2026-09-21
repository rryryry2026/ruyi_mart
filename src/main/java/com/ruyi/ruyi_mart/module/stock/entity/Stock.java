package com.ruyi.ruyi_mart.module.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**商品库存 stock 表的镜像。一个商品一条记录。*/
@Data
@TableName("stock")
public class Stock {

    /**
     * 主键即商品ID（一个商品一条库存记录）。
     * 用 INPUT 而不是 AUTO：这个值是按商品ID传进来的，不是数据库自增。
     * 标成 AUTO 会让 MyBatis-Plus 以为"插入后要回填自增主键"，
     * 语义对不上（也埋着"哪天不传 productId 插入、数据库自己编一个id"的坑）。
     */
    @TableId(type = IdType.INPUT)
    private Long productId;

    /**库存总量*/
    private Integer total;

    /**可用库存*/
    private Integer available;

    /**已被未支付订单预扣的数量*/
    private Integer locked;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
