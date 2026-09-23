package com.ruyi.ruyi_mart.module.address.entity;

import jakarta.validation.constraints.NotBlank;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 数据库address表在java里的镜像
 * 表的一行数据<-->一个address对象
 */
@Data
@TableName("address")
public class Address {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**收货人（必填）*/
    @NotBlank(message = "收货人不能为空")
    private String receiver;

    /**联系电话（必填）*/
    @NotBlank(message = "联系电话不能为空")
    private String phone;

    private String province;

    private String city;

    private String district;

    /**详细地址（必填）*/
    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
