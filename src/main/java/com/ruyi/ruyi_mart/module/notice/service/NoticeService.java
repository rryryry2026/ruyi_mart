package com.ruyi.ruyi_mart.module.notice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.notice.dto.NoticeDTO;
import com.ruyi.ruyi_mart.module.notice.entity.Notice;

import java.util.List;

/**公告业务接口，声明方法。*/
public interface NoticeService extends IService<Notice> {

    /** 管理端列表：全部公告，按 sort 升序 */
    List<Notice> listAll();

    /** 消费端列表：仅启用状态，按 sort 升序 */
    List<Notice> listEnabled();

    /** 新增公告 */
    void addNotice(NoticeDTO dto);

    /** 修改公告（只更新传入的非空字段） */
    void updateNotice(Long id, NoticeDTO dto);

    /** 删除公告 */
    void deleteNotice(Long id);

    /** 单独启用 / 禁用 */
    void updateStatus(Long id, Integer status);
}
