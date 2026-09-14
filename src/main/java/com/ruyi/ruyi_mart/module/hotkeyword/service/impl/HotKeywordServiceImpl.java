package com.ruyi.ruyi_mart.module.hotkeyword.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.hotkeyword.dto.HotKeywordDTO;
import com.ruyi.ruyi_mart.module.hotkeyword.entity.HotKeyword;
import com.ruyi.ruyi_mart.module.hotkeyword.mapper.HotKeywordMapper;
import com.ruyi.ruyi_mart.module.hotkeyword.service.HotKeywordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HotKeywordServiceImpl extends ServiceImpl<HotKeywordMapper, HotKeyword> implements HotKeywordService {

    private static final int STATUS_ENABLED = 1;

    @Override
    public List<HotKeyword> listAll() {
        return lambdaQuery().orderByAsc(HotKeyword::getSort).orderByDesc(HotKeyword::getId).list();
    }

    @Override
    public List<HotKeyword> listEnabled(Integer limit) {
        var query = lambdaQuery()
                .eq(HotKeyword::getStatus, STATUS_ENABLED)
                .orderByAsc(HotKeyword::getSort)
                .orderByDesc(HotKeyword::getId);
        if (limit != null && limit > 0) {
            query = query.last("limit " + Math.min(limit, 50));
        }
        return query.list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addKeyword(HotKeywordDTO dto) {
        HotKeyword entity = new HotKeyword();
        entity.setKeyword(dto.getKeyword());
        entity.setSort(dto.getSort() != null ? dto.getSort() : 0);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : STATUS_ENABLED);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateKeyword(Long id, HotKeywordDTO dto) {
        HotKeyword existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FIND, "热搜词不存在");
        }
        if (dto.getKeyword() != null) existing.setKeyword(dto.getKeyword());
        if (dto.getSort() != null) existing.setSort(dto.getSort());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKeyword(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.NOT_FIND, "热搜词不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null) {
            throw new BusinessException(ResultCode.FAIL, "状态不能为空");
        }
        boolean updated = lambdaUpdate()
                .eq(HotKeyword::getId, id)
                .set(HotKeyword::getStatus, status)
                .set(HotKeyword::getUpdateTime, LocalDateTime.now())
                .update();
        if (!updated) {
            throw new BusinessException(ResultCode.NOT_FIND, "热搜词不存在");
        }
    }
}
