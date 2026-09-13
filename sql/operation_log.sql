-- ============================================================
-- 操作日志表（管理端审计日志）
-- 由 OperationLogAspect 切面自动写入：记录所有需要 ADMIN 权限的写操作
-- ============================================================

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`        BIGINT       DEFAULT NULL COMMENT '操作人用户ID',
  `username`       VARCHAR(64)  DEFAULT NULL COMMENT '操作人用户名（冗余存储，用户改名/删除后日志仍可读）',
  `module`         VARCHAR(64)  DEFAULT NULL COMMENT '业务模块，如 商品管理',
  `action`         VARCHAR(64)  DEFAULT NULL COMMENT '操作类型，如 新增/修改/删除',
  `request_uri`    VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
  `request_method` VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP 方法',
  `class_method`   VARCHAR(255) DEFAULT NULL COMMENT '执行的类#方法，便于定位代码',
  `params`         VARCHAR(2000) DEFAULT NULL COMMENT '请求参数（已脱敏并截断）',
  `success`        TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功：1成功 0失败',
  `error_msg`      VARCHAR(500) DEFAULT NULL COMMENT '失败时的异常信息',
  `ip`             VARCHAR(64)  DEFAULT NULL COMMENT '客户端IP',
  `cost_ms`        BIGINT       DEFAULT NULL COMMENT '耗时（毫秒）',
  `create_time`    DATETIME     DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`id`),
  -- 日志页默认按时间倒序 + 可按操作人筛选，这两个索引覆盖主要查询
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端操作日志';
