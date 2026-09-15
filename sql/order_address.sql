-- ============================================================
-- 如意商城 - 订单收货地址快照
--
-- 背景：order_info 此前没有任何地址字段，下单时前端虽然选了收货地址，
-- 但既没传给后端、后端也没地方存，导致商家发货时拿不到收货信息。
--
-- 设计取舍：存快照而不是只存 address_id。
-- 地址表里的记录可能被用户改掉或删除，历史订单必须保留下单当时的收货信息，
-- 否则改一次地址，所有历史订单的收货信息都会跟着变。
--
-- 本文件可重复执行（字段已存在则跳过）。
-- ============================================================

SET @ddl = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'receiver') = 0,
        CONCAT(
            'ALTER TABLE `order_info`',
            '  ADD COLUMN `receiver` VARCHAR(50)  NULL COMMENT ''收货人（下单时快照）''',
            ', ADD COLUMN `phone` VARCHAR(20)     NULL COMMENT ''收货人手机号（下单时快照）''',
            ', ADD COLUMN `province` VARCHAR(50)  NULL COMMENT ''省（下单时快照）''',
            ', ADD COLUMN `city` VARCHAR(50)      NULL COMMENT ''市（下单时快照）''',
            ', ADD COLUMN `district` VARCHAR(50)  NULL COMMENT ''区县（下单时快照）''',
            ', ADD COLUMN `detail_address` VARCHAR(200) NULL COMMENT ''详细地址（下单时快照）'''
        ),
        'SELECT ''order_info 已有收货地址字段，跳过'' AS msg'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 验证：新字段是否就位
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'order_info'
  AND COLUMN_NAME IN ('receiver', 'phone', 'province', 'city', 'district', 'detail_address')
ORDER BY ORDINAL_POSITION;
