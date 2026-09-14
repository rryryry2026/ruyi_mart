package com.ruyi.ruyi_mart.module.hotkeyword.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.ruyi_mart.module.hotkeyword.dto.HotKeywordDTO;
import com.ruyi.ruyi_mart.module.hotkeyword.entity.HotKeyword;

import java.util.List;

public interface HotKeywordService extends IService<HotKeyword> {

    /** 管理端列表：全部关键词，按 sort 升序 */
    List<HotKeyword> listAll();

    /** 消费端列表：仅启用状态，按 sort 升序；limit 为可选条数上限 */
    List<HotKeyword> listEnabled(Integer limit);

    /** 新增 */
    void addKeyword(HotKeywordDTO dto);

    /** 修改（只更新传入的非空字段） */
    void updateKeyword(Long id, HotKeywordDTO dto);

    /** 删除 */
    void deleteKeyword(Long id);

    /** 单独启用 / 禁用 */
    void updateStatus(Long id, Integer status);
}
