package com.ruyi.ruyi_mart.module.user.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RefreshRequest implements Serializable {
    private String refreshToken;
}
