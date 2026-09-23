package com.ruyi.ruyi_mart.module.product.entity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**“product”表的镜像。*/
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**商品名称（必填）*/
    @NotBlank(message = "商品名称不能为空")
    private String name;
    /**售价（必填，不能为负）*/
    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.00", message = "售价不能为负数")
    private BigDecimal price;
    private String imageUrl;
    private String description;
    private Integer status;
    private Long categoryId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
