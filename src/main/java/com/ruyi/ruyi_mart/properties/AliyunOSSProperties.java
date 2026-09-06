package com.ruyi.ruyi_mart.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.aliyun.oss")
@Data
public class AliyunOSSProperties {

    private String endpoint;          // OSS 地域节点，如 oss-cn-hangzhou.aliyuncs.com
    private String accessKeyId;       // 阿里云 AccessKeyId
    private String accessKeySecret;   // 阿里云 AccessKeySecret
    private String bucketName;        // 存储桶名称
}
