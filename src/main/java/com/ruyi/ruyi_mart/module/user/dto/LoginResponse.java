package com.ruyi.ruyi_mart.module.user.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginResponse implements Serializable {

    private Long userId;
    private String username;
    private String nickname;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
