package com.ruyi.ruyi_mart.module.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端操作日志。
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人用户ID */
    private Long userId;

    /** 操作人用户名（冗余存储） */
    private String username;

    /** 业务模块，如「商品管理」 */
    private String module;

    /** 操作类型，如「新增」 */
    private String action;

    private String requestUri;

    private String requestMethod;

    /** 执行的类#方法，便于从日志定位到代码 */
    private String classMethod;

    /** 请求参数（已脱敏、已截断） */
    private String params;

    /** 是否成功：1成功 0失败 */
    private Integer success;

    private String errorMsg;

    private String ip;

    /** 耗时（毫秒） */
    private Long costMs;

    private LocalDateTime createTime;
}
