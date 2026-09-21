package com.ruyi.ruyi_mart.module.notice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.notice.dto.NoticeDTO;
import com.ruyi.ruyi_mart.module.notice.entity.Notice;
import com.ruyi.ruyi_mart.module.notice.mapper.NoticeMapper;
import com.ruyi.ruyi_mart.module.notice.service.NoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**公告业务模块的具体实现。*/
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    private static final int STATUS_ENABLED = 1;

    /**全部公告列表*/
    @Override
    public List<Notice> listAll() {
        return lambdaQuery()
                .orderByAsc(Notice::getSort)
                .orderByDesc(Notice::getId)
                .list();
    }

    /**仅启用的公告。*/
    @Override
    public List<Notice> listEnabled() {
        return lambdaQuery()
                .eq(Notice::getStatus, STATUS_ENABLED)
                .orderByAsc(Notice::getSort)
                .orderByDesc(Notice::getId)
                .list();
    }

    /**添加公告。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addNotice(NoticeDTO dto) {
        Notice notice = new Notice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setSort(dto.getSort() != null ? dto.getSort() : 0);
        notice.setStatus(dto.getStatus() != null ? dto.getStatus() : STATUS_ENABLED);
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        save(notice);
    }

    /**修改公告*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(Long id, NoticeDTO dto) {
        Notice existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FIND, "公告不存在");
        }
        if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
        if (dto.getContent() != null) existing.setContent(dto.getContent());
        if (dto.getSort() != null) existing.setSort(dto.getSort());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    /**删除公告。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.NOT_FIND, "公告不存在");
        }
    }

    /**修改公告状态。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null) {
            throw new BusinessException(ResultCode.FAIL, "状态不能为空");
        }
        boolean updated = lambdaUpdate()
                .eq(Notice::getId, id)
                .set(Notice::getStatus, status)
                .set(Notice::getUpdateTime, LocalDateTime.now())
                .update();
        if (!updated) {
            throw new BusinessException(ResultCode.NOT_FIND, "公告不存在");
        }
    }
}
