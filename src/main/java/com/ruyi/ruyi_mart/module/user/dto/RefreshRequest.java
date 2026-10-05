package com.ruyi.ruyi_mart.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class RefreshRequest implements Serializable {

    /**续期令牌：没有它这个接口什么都做不了，直接在参数层拦下空值*/
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
