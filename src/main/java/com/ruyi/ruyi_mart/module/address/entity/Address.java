package com.ruyi.ruyi_mart.module.address.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    /**收货人（必填；长度与列一致，避免非严格模式下被静默截断）*/
    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人不能超过50个字符")
    private String receiver;

    /**联系电话（必填，11 位手机号，与注册口径一致）*/
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 50, message = "省份不能超过50个字符")
    private String province;

    @Size(max = 50, message = "城市不能超过50个字符")
    private String city;

    @Size(max = 50, message = "区县不能超过50个字符")
    private String district;

    /**详细地址（必填；列宽 200）*/
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址不能超过200个字符")
    private String detailAddress;

    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
