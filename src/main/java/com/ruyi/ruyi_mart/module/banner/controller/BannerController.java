package com.ruyi.ruyi_mart.module.banner.controller;

import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.banner.dto.BannerDTO;
import com.ruyi.ruyi_mart.module.banner.dto.BannerSortDTO;
import com.ruyi.ruyi_mart.module.banner.dto.BannerStatusDTO;
import com.ruyi.ruyi_mart.module.banner.entity.Banner;
import com.ruyi.ruyi_mart.module.banner.service.BannerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/enabled")
    public Result<List<Banner>> listEnabled(){
        return Result.success(bannerService.listEnabled());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Banner>> listAll(){
        return Result.success(bannerService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> add(@Valid @RequestBody BannerDTO dto){
        bannerService.addBanner(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id,@Valid @RequestBody BannerDTO dto){
        bannerService.updateBanner(id,dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id){
        bannerService.deleteBanner(id);
        return Result.success();
    }

    @PostMapping("/sort")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> sort(@Valid @RequestBody BannerSortDTO dto){
        bannerService.sortBanners(dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody BannerStatusDTO dto){
        bannerService.updateStatus(id,dto);
        return Result.success();
    }


}
