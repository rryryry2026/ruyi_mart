package com.ruyi.ruyi_mart.common.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.ruyi.ruyi_mart.properties.AliyunOSSProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class AliyunOSSUtils {

    @Autowired
    private AliyunOSSProperties aliyunOSSProperties;

    public String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;

        OSS ossClient = new OSSClientBuilder().build(
                aliyunOSSProperties.getEndpoint(),
                aliyunOSSProperties.getAccessKeyId(),
                aliyunOSSProperties.getAccessKeySecret()
        );

        try {
            ossClient.putObject(aliyunOSSProperties.getBucketName(), fileName, file.getInputStream());
        } catch (Exception e) {
            log.error("文件上传到阿里云 OSS 失败: {}", e);
            throw new RuntimeException("文件上传失败:" + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        StringBuilder url = new StringBuilder("https://");
        url.append(aliyunOSSProperties.getBucketName())
                .append(".")
                .append(aliyunOSSProperties.getEndpoint())
                .append("/")
                .append(fileName);

        log.info("文件上传成功，访问路径为: {}", url);
        return url.toString();
    }
}
