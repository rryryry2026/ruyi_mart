package com.ruyi.ruyi_mart.module.notice.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.notice.dto.NoticeDTO;
import com.ruyi.ruyi_mart.module.notice.entity.Notice;
import com.ruyi.ruyi_mart.module.notice.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告。
 * 读写权限的划分与 BannerController 一致：
 * 读接口放行给消费端，写接口用 @PreAuthorize 限管理员。
 */
/**公告业务模块的接口层。*/
@RestController
@RequestMapping("/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /** 消费端：启用中的公告，按 sort 升序 */
    @GetMapping("/enabled")
    public Result<List<Notice>> listEnabled() {
        return Result.success(noticeService.listEnabled());
    }

    /** 管理端：全部公告 */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Notice>> listAll() {
        return Result.success(noticeService.listAll());
    }

    /** 管理端：新增 */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> add(@Valid @RequestBody NoticeDTO dto) {
        noticeService.addNotice(dto);
        return Result.success();
    }

    /** 管理端：修改 */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeDTO dto) {
        noticeService.updateNotice(id, dto);
        return Result.success();
    }

    /** 管理端：删除 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return Result.success();
    }

    /** 管理端：启用 / 禁用 */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        noticeService.updateStatus(id, status);
        return Result.success();
    }
}
