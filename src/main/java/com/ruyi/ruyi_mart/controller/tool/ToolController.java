package com.ruyi.ruyi_mart.controller.tool;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.common.util.AliyunOSSUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Tag(name = "工具")
@Slf4j
public class ToolController {

    @Autowired
    private AliyunOSSUtils aliyunOSSUtils;

    // 接收前端上传的图片文件，转存到 OSS，返回可访问的 URL
    @PostMapping("/upload/image")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "图片上传")
    public Result<String> upload(MultipartFile file) {
        log.info("图片上传: {}", file.getOriginalFilename());
        String url = aliyunOSSUtils.upload(file);
        return Result.success(url);
    }
}
