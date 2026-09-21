package com.ruyi.ruyi_mart.controller.tool;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.common.util.AliyunOSSUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**图片上传的工具类。*/
@RestController
@RequestMapping("/api")
@Tag(name = "工具")
@Slf4j
public class ToolController {

    @Autowired
    private AliyunOSSUtils aliyunOSSUtils;

    /**
     * 图片上传：转存到 OSS，返回可访问的 URL。
     *
     * 不再限管理员——消费端发表评价需要晒图，个人端必须能上传。
     * 但放开的同时，文件校验改由 AliyunOSSUtils.upload 承担
     * （非空、大小上限、类型白名单、文件头校验），
     * 否则就等于对外开放了一个任意文件上传入口。
     * 本接口未加入 SecurityConfig 的放行名单，匿名访问会被拦成 401。
     *
     * @param dir 存放目录，仅允许 review / product / banner / common
     */
    /**
     * 上传图片到oss，返回访问的url。
     * 文件参数靠类型自动识别，普通参数靠注解显示声明。
     */
    @PostMapping("/upload/image")
    @Operation(summary = "图片上传")
    public Result<String> upload(MultipartFile file,
                                 @RequestParam(required = false, defaultValue = "common") String dir) {
        //用{}占位符，真要输出时才做替换。避免白创建临时对象，不输出日志时不浪费运算。
        log.info("图片上传: dir={}, name={}, size={}", dir,
                file == null ? null : file.getOriginalFilename(),
                file == null ? 0 : file.getSize());
        return Result.success(aliyunOSSUtils.upload(file, dir));
    }
}
