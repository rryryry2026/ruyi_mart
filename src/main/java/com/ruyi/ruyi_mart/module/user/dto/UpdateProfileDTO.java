package com.ruyi.ruyi_mart.module.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileDTO {

    private String nickname;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
