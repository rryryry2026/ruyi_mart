package com.ruyi.ruyi_mart.common.util;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.comm.Protocol;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.properties.AliyunOSSProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class AliyunOSSUtils {

    @Autowired
    private AliyunOSSProperties aliyunOSSProperties;

 /**大小上限，multipart 配的 10MB 是容器兜底。*/
    private static final long MAX_SIZE = 5 * 1024 * 1024L;

    /**
     * 允许的图片类型 -> 落 OSS 时使用的后缀。
     * 后缀由校验过的类型决定，不沿用原始文件名里的后缀，
     * 避免把 .jsp / .html 这类文件名带进来。
     */
    /**类型白名单。*/
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    /** 允许的存放目录，避免调用方随意拼路径 */
    /**目录白名单。*/
    private static final Set<String> ALLOWED_DIRS = Set.of("review", "product", "banner", "common");

    /**
     * OSS 域名的连接超时。
     * SDK 默认约 50 秒，一旦网络抖动人会以为页面卡死，这里压到 10 秒快速失败。
     */
    /**超时时间设置。*/
    private static final int CONNECT_TIMEOUT_MS = 10 * 1000;
    private static final int SOCKET_TIMEOUT_MS = 15 * 1000;

    /** 上传重试次数。OSS 域名对应多个 IP，个别 IP 可能不可达，重试会重新建连并重新解析 */
    /**重试次数。*/
    private static final int MAX_ATTEMPTS = 2;

    /**
     * 上传图片到 OSS。
     * 消费端也能调用，因此这里做完整校验：
     * 非空 / 大小上限 / 类型白名单 / 文件头（magic bytes）。
     */
    public String upload(MultipartFile file, String dir) {
        // 配置模板里 OSS 密钥是留空的（需用环境变量注入），这里先给出明确提示，
        // 否则只会报一句笼统的"上传失败"，看不出是配置问题
        //检查配置问题。
        if (!StringUtils.hasText(aliyunOSSProperties.getAccessKeyId())
                || !StringUtils.hasText(aliyunOSSProperties.getAccessKeySecret())
                || !StringUtils.hasText(aliyunOSSProperties.getBucketName())
                || !StringUtils.hasText(aliyunOSSProperties.getEndpoint())) {
            throw new BusinessException(ResultCode.FAIL,
                    "OSS 未配置：请设置环境变量 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET");
        }
        //检查文件非空。
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FAIL, "上传文件不能为空");
        }
        //检查文件大小。
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.FAIL, "图片不能超过 5MB");
        }

        //类型白名单->确定后缀。
        String extension = ALLOWED_TYPES.get(normalizeContentType(file.getContentType()));
        if (extension == null) {
            throw new BusinessException(ResultCode.FAIL, "只支持 jpg / png / webp / gif 格式的图片");
        }

        // 一次性读出字节：既用于文件头校验，也用于上传。
        // 不调两次 file.getInputStream()——部分实现不保证流可重复读。
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BusinessException(ResultCode.FAIL, "读取上传文件失败");
        }

        if (!matchesMagicBytes(bytes, extension)) {
            // Content-Type 由客户端提供、可以伪造，文件头对不上就说明不是真图片
            throw new BusinessException(ResultCode.FAIL, "文件内容不是有效的图片");
        }

        String folder = ALLOWED_DIRS.contains(dir) ? dir : "common";
        String fileName = folder + "/" + UUID.randomUUID() + extension;

        putObjectWithRetry(fileName, bytes);

        String url = "https://" + aliyunOSSProperties.getBucketName()
                + "." + hostOf(aliyunOSSProperties.getEndpoint())
                + "/" + fileName;

        log.info("文件上传成功，访问路径为: {}", url);
        return url;
    }

    private void putObjectWithRetry(String fileName, byte[] bytes) {
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        // 显式指定 HTTPS：endpoint 不带协议时 SDK 默认走 HTTP(80)，
        // 部分网络下 80 会被拦，且明文传输本身也不合适
        conf.setProtocol(Protocol.HTTPS);
        conf.setConnectionTimeout(CONNECT_TIMEOUT_MS);
        conf.setSocketTimeout(SOCKET_TIMEOUT_MS);
        conf.setMaxErrorRetry(0); // 重试逻辑放在下面自己做，便于按次重建客户端

        String endpoint = resolveEndpoint(aliyunOSSProperties.getEndpoint());
        Exception last = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // 每次重建客户端：会重新建立连接、重新解析域名，
            // 上一次解析到的 IP 不可达时，这一次有机会换到可达的 IP
            OSS ossClient = new OSSClientBuilder().build(
                    endpoint,
                    aliyunOSSProperties.getAccessKeyId(),
                    aliyunOSSProperties.getAccessKeySecret(),
                    conf
            );
            try {
                ossClient.putObject(aliyunOSSProperties.getBucketName(), fileName,
                        new ByteArrayInputStream(bytes));
                return;
            } catch (Exception e) {
                last = e;
                log.warn("文件上传到阿里云 OSS 失败（第 {}/{} 次）: {}",
                        attempt, MAX_ATTEMPTS, e.getMessage());
            } finally {
                ossClient.shutdown();
            }
        }

        log.error("文件上传到阿里云 OSS 最终失败: {}", last == null ? "unknown" : last.getMessage(), last);
        throw new BusinessException(ResultCode.ERROR, "文件上传失败，请稍后重试");
    }

    /** SDK 需要带协议的地址；配置里填的是裸域名时补上 https:// */
    private String resolveEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return endpoint;
        }
        String e = endpoint.trim();
        if (e.startsWith("http://") || e.startsWith("https://")) {
            return e;
        }
        return "https://" + e;
    }

    /** 拼访问地址时要去掉协议前缀，否则会拼出 https://bucket.https://xxx 这种坏地址 */
    private String hostOf(String endpoint) {
        if (endpoint == null) {
            return "";
        }
        return endpoint.trim().replaceFirst("^https?://", "");
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semi = contentType.indexOf(';');
        String value = semi > 0 ? contentType.substring(0, semi) : contentType;
        return value.trim().toLowerCase();
    }

    /** 按文件头判断真实格式，防止把伪装成图片的文件传上去 */
    private boolean matchesMagicBytes(byte[] bytes, String extension) {
        switch (extension) {
            case ".jpg":
                return bytes.length >= 3
                        && (bytes[0] & 0xFF) == 0xFF
                        && (bytes[1] & 0xFF) == 0xD8
                        && (bytes[2] & 0xFF) == 0xFF;
            case ".png":
                return bytes.length >= 8
                        && (bytes[0] & 0xFF) == 0x89
                        && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                        && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A
                        && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A;
            case ".gif":
                return bytes.length >= 6
                        && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                        && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9')
                        && bytes[5] == 'a';
            case ".webp":
                return bytes.length >= 12
                        && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                        && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default:
                return false;
        }
    }
}
