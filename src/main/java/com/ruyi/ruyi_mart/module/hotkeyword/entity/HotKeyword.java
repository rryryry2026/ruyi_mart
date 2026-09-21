package com.ruyi.ruyi_mart.module.hotkeyword.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 热搜词。
 * 消费端首页搜索栏的滚动热词、搜索页的"热门搜索"标签都用它。
 * 此前首页拿一级分类名顶替、搜索页拿商品名派生，都不是真实的热搜数据。
 */
/**hot_keyword的镜像。*/
@Data
@TableName("hot_keyword")
public class HotKeyword {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关键词 */
    @TableField("`keyword`")
    private String keyword;

    /** 排序，数字越小越靠前 */
    private Integer sort = 0;

    /** 状态：1 启用，0 禁用 */
    private Integer status = 1;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
