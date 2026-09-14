-- ============================================================
-- 如意商城 - 公告表
-- 消费端首页的滚动公告栏数据来源
-- ============================================================

CREATE TABLE IF NOT EXISTS `notice`
(
    `id`          BIGINT AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    `title`       VARCHAR(100) NOT NULL COMMENT '公告标题（滚动栏显示的就是它）',
    `content`     VARCHAR(2000) NULL COMMENT '公告内容，可选',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序，数字越小越靠前',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '公告';

-- ------------------------------------------------------------
-- 初始化数据
-- ------------------------------------------------------------
INSERT INTO `notice` (`title`, `content`, `sort`, `status`)
SELECT '新用户首单立减 20 元', '注册即送新人券，首单可用，详见领券中心。', 1, 1
WHERE NOT EXISTS(SELECT 1 FROM `notice` WHERE `title` = '新用户首单立减 20 元');

INSERT INTO `notice` (`title`, `content`, `sort`, `status`)
SELECT '满 99 元包邮', '全站满 99 元免运费，偏远地区除外。', 2, 1
WHERE NOT EXISTS(SELECT 1 FROM `notice` WHERE `title` = '满 99 元包邮');

INSERT INTO `notice` (`title`, `content`, `sort`, `status`)
SELECT '产地直采 品质保障', '生鲜类商品产地直采，支持坏果包赔。', 3, 1
WHERE NOT EXISTS(SELECT 1 FROM `notice` WHERE `title` = '产地直采 品质保障');

-- 验证
SELECT `id`, `title`, `sort`, `status` FROM `notice` ORDER BY `sort`;
