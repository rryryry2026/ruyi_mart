-- ============================================================
-- 如意商城 - 热搜词表
-- 消费端首页搜索栏的滚动热词、搜索页的"热门搜索"标签数据来源
--
-- 注意：关键词必须能被商品名 LIKE 到（搜索走 name LIKE '%kw%'），
-- 否则用户点进去是空结果。下面这些词都是在现有商品名里逐个核对过的，
-- 文件末尾的验证查询会打印每个词能命中的商品数，0 就必须换词。
--
-- 重复执行安全：keyword 上有唯一索引，用 ON DUPLICATE KEY UPDATE
-- 让本文件成为排序与状态的唯一来源（重跑即校正）。
-- ============================================================

CREATE TABLE IF NOT EXISTS `hot_keyword`
(
    `id`          BIGINT AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    `keyword`     VARCHAR(30) NOT NULL COMMENT '关键词',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序，数字越小越靠前',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT `uk_hot_keyword` UNIQUE (`keyword`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '热搜词';

-- ------------------------------------------------------------
-- 初始化数据
-- ------------------------------------------------------------
INSERT INTO `hot_keyword` (`keyword`, `sort`, `status`) VALUES
    ('蓝牙耳机',   1, 1),
    ('羽绒服',     2, 1),
    ('运动休闲鞋', 3, 1),
    ('短袖T恤',    4, 1),
    ('冰箱',       5, 1),
    ('数码相机',   6, 1),
    ('双人床',     7, 1),
    ('口红',       8, 1),
    ('奶粉',       9, 1),
    ('草莓',      10, 1),
    ('篮球',      11, 1)
ON DUPLICATE KEY UPDATE `sort` = VALUES(`sort`), `status` = VALUES(`status`);

-- ------------------------------------------------------------
-- 验证：关键词 + 能搜到的商品数（0 说明该词点进去是空结果，需要换词）
-- ------------------------------------------------------------
SELECT k.`keyword`,
       k.`sort`,
       (SELECT COUNT(*) FROM `product` p WHERE p.`name` LIKE CONCAT('%', k.`keyword`, '%')) AS `match_count`
FROM `hot_keyword` k
ORDER BY k.`sort`;
