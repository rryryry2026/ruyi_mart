package com.ruyi.ruyi_mart.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**从配置文件中找jwt开头的那一段，将配置里的项对应到该类里的字段  jwt.secret->secret。*/
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private Long accessExpire;
    private Long refreshExpire;
}
