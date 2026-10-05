package com.ruyi.ruyi_mart.module.banner.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.banner.dto.BannerDTO;
import com.ruyi.ruyi_mart.module.banner.dto.BannerSortDTO;
import com.ruyi.ruyi_mart.module.banner.dto.BannerStatusDTO;
import com.ruyi.ruyi_mart.module.banner.entity.Banner;
import com.ruyi.ruyi_mart.module.banner.service.BannerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**轮播图的管理接口层。*/
@RestController
@RequestMapping("/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    /**返回已启用的轮播图，（用户）*/
    @GetMapping("/enabled")
    public Result<List<Banner>> listEnabled()
    {
        return Result.success(bannerService.listEnabled());
    }

    /**返回全部轮播图，权限校验。（管理员）*/
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Banner>> listAll(){
        return Result.success(bannerService.listAll());
    }

    /**新增轮播图。（管理员）*/
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> add(@Valid @RequestBody BannerDTO dto){
        bannerService.addBanner(dto);
        return Result.success();
    }

    /**更新轮播图。（管理员）*/
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id,@Valid @RequestBody BannerDTO dto){
        bannerService.updateBanner(id,dto);
        return Result.success();
    }

    /**删除轮播图。（管理员）*/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id){
        bannerService.deleteBanner(id);
        return Result.success();
    }

    /**轮播图排序。（管理员）*/
    @PostMapping("/sort")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> sort(@Valid @RequestBody BannerSortDTO dto){
        bannerService.sortBanners(dto);
        return Result.success();
    }

    /**更改轮播图状态。（管理员）*/
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody BannerStatusDTO dto){
        bannerService.updateStatus(id,dto);
        return Result.success();
    }


}
