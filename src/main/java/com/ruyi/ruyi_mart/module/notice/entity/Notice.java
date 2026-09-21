package com.ruyi.ruyi_mart.module.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告。
 * 消费端首页有一条滚动公告栏，此前是拿轮播图标题顶替的（后端没有对应表），
 * 这个模块把它补上。
 */
/**notice表的镜像。*/
@Data
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题（滚动栏显示的就是它） */
    private String title;

    /** 公告内容，可选，点击查看详情时用 */
    private String content;

    /** 排序，数字越小越靠前 */
    private Integer sort = 0;

    /** 状态：1 启用，0 禁用 */
    private Integer status = 1;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
