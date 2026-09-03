package com.ruyi.ruyi_mart.module.banner.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.banner.dto.BannerDTO;
import com.ruyi.ruyi_mart.module.banner.dto.BannerSortDTO;
import com.ruyi.ruyi_mart.module.banner.dto.BannerStatusDTO;
import com.ruyi.ruyi_mart.module.banner.entity.Banner;

import java.util.List;

public interface BannerService extends IService<Banner> {

    /** 后台列表:全部轮播图,按 sort 升序 */
    List<Banner> listAll();

    /** 前台列表:仅启用状态,按 sort 升序 */
    List<Banner> listEnabled();

    /** 新增轮播图 */
    void addBanner(BannerDTO dto);

    /** 修改轮播图(局部更新非空字段) */
    void updateBanner(Long id, BannerDTO dto);

    /** 删除轮播图 */
    void deleteBanner(Long id);

    /** 批量排序:按传入 id 顺序重排 sort */
    void sortBanners(BannerSortDTO dto);

    /** 单独启用/禁用 */
    void updateStatus(Long id, BannerStatusDTO dto);


}
