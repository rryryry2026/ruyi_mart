package com.ruyi.ruyi_mart.module.category.entity;

import jakarta.validation.constraints.NotBlank;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**category商品分类表的镜像。又是树的节点。*/
@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;
    /**分类名称（必填）*/
    @NotBlank(message = "分类名称不能为空")
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
