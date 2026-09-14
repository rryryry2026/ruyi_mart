package com.ruyi.ruyi_mart.module.hotkeyword.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.hotkeyword.dto.HotKeywordDTO;
import com.ruyi.ruyi_mart.module.hotkeyword.entity.HotKeyword;
import com.ruyi.ruyi_mart.module.hotkeyword.service.HotKeywordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 热搜词。
 * 读写权限的划分与 BannerController 一致：
 * 读接口放行给消费端，写接口用 @PreAuthorize 限管理员。
 */
@RestController
@RequestMapping("/hot-keyword")
public class HotKeywordController {

    @Autowired
    private HotKeywordService hotKeywordService;

    /** 消费端：启用中的热搜词，按 sort 升序；limit 可选 */
    @GetMapping("/enabled")
    public Result<List<HotKeyword>> listEnabled(@RequestParam(required = false) Integer limit) {
        return Result.success(hotKeywordService.listEnabled(limit));
    }

    /** 管理端：全部热搜词 */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<HotKeyword>> listAll() {
        return Result.success(hotKeywordService.listAll());
    }

    /** 管理端：新增 */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> add(@Valid @RequestBody HotKeywordDTO dto) {
        hotKeywordService.addKeyword(dto);
        return Result.success();
    }

    /** 管理端：修改 */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody HotKeywordDTO dto) {
        hotKeywordService.updateKeyword(id, dto);
        return Result.success();
    }

    /** 管理端：删除 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        hotKeywordService.deleteKeyword(id);
        return Result.success();
    }

    /** 管理端：启用 / 禁用 */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        hotKeywordService.updateStatus(id, status);
        return Result.success();
    }
}
