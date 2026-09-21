package com.ruyi.ruyi_mart.module.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.ruyi_mart.module.notice.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

/**公告业务模块的mapper层。*/
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}
