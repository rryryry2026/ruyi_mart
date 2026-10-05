package com.ruyi.ruyi_mart.module.banner.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.banner.dto.BannerDTO;
import com.ruyi.ruyi_mart.module.banner.dto.BannerSortDTO;
import com.ruyi.ruyi_mart.module.banner.dto.BannerStatusDTO;
import com.ruyi.ruyi_mart.module.banner.entity.Banner;
import com.ruyi.ruyi_mart.module.banner.enums.BannerStatus;
import com.ruyi.ruyi_mart.module.banner.mapper.BannerMapper;
import com.ruyi.ruyi_mart.module.banner.service.BannerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**轮播图业务的真正实现。*/
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    /**查全部轮播图。*/
    @Override
    public List<Banner> listAll(){
        return lambdaQuery().orderByAsc(Banner::getSort).orderByAsc(Banner::getId).list();
    }

    /**查已启用的轮播图。*/
    @Override
    public List<Banner> listEnabled(){
        return lambdaQuery()
                .eq(Banner::getStatus, BannerStatus.ACTIVE)
                .orderByAsc(Banner::getSort)
                .orderByAsc(Banner::getId)
                .list();
    }

    /**新增轮播图。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBanner(BannerDTO dto){
        // imageUrl 必填校验从 DTO 挪到这里：列是 NOT NULL，且只有新建这一条路径必须传
        if(dto.getImageUrl() == null || dto.getImageUrl().isBlank()){
            throw new BusinessException(ResultCode.FAIL, "图片URL不能为空");
        }
        Banner banner = new Banner();
        banner.setTitle(dto.getTitle());
        banner.setImageUrl(dto.getImageUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setLinkType(dto.getLinkType());
        banner.setSort(dto.getSort() != null ? dto.getSort() : 0);
        banner.setStatus(dto.getStatus() != null ? dto.getStatus() : BannerStatus.ACTIVE);
        banner.setCreateTime(LocalDateTime.now());
        banner.setUpdateTime(LocalDateTime.now());
        save(banner);
    }

    /**更新轮播图。（局部更新）*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBanner(Long id, BannerDTO dto){
        Banner existing = getById(id);
        if(existing == null){
            throw new BusinessException(ResultCode.NOT_FIND, "轮播图不存在");
        }
        if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
        if (dto.getImageUrl() != null) existing.setImageUrl(dto.getImageUrl());
        if (dto.getLinkUrl() != null) existing.setLinkUrl(dto.getLinkUrl());
        if (dto.getLinkType() != null) existing.setLinkType(dto.getLinkType());
        if (dto.getSort() != null) existing.setSort(dto.getSort());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    /**删除轮播图。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBanner(Long id){
        if(!removeById(id)){
            throw new BusinessException(ResultCode.NOT_FIND, "轮播图不存在");
        }
    }

    /**轮播图批量排序。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortBanners(BannerSortDTO dto){
        List<Long> ids = dto.getIds();
        // 校验所有 id 都存在：updateBatchById 对不存在的 id 静默跳过，
        // 前端会以为排序成功了，实际序列里少了几张图
        long existing = this.lambdaQuery().in(Banner::getId, ids).count();
        if(existing != ids.size()){
            throw new BusinessException(ResultCode.NOT_FIND, "存在无效的轮播图ID，排序失败");
        }
        List<Banner> banners = new ArrayList<>(ids.size());
        for(int i = 0;i < ids.size();i++){
            Banner b = new Banner();
            b.setId(ids.get(i));
            b.setSort(i);
            banners.add(b);
        }
        updateBatchById(banners);
    }

    /**修改轮播图状态。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, BannerStatusDTO dto){
        boolean updated = lambdaUpdate()
                .eq(Banner::getId,id)
                .set(Banner::getStatus,dto.getStatus())
                .set(Banner::getUpdateTime,LocalDateTime.now())
                .update();
        if(!updated){
            throw new BusinessException(ResultCode.NOT_FIND, "轮播图不存在");
        }
    }
}
