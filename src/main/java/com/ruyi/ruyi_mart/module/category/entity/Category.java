package com.ruyi.ruyi_mart.module.category.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;
    private String iconUrl;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<Category> children;
}
