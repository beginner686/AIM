USE ai_link;

SET @db_name = DATABASE();

-- 1) Add generated column to enforce "at most one UNPAID payment per order"
SET @sql_add_unpaid_guard = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'platform_payment'
        AND COLUMN_NAME = 'unpaid_guard'
    ),
    'SELECT 1',
    'ALTER TABLE platform_payment ADD COLUMN unpaid_guard BIGINT GENERATED ALWAYS AS (CASE WHEN deleted = 0 AND status = ''UNPAID'' THEN order_id ELSE NULL END) STORED'
  )
);
PREPARE stmt_add_unpaid_guard FROM @sql_add_unpaid_guard;
EXECUTE stmt_add_unpaid_guard;
DEALLOCATE PREPARE stmt_add_unpaid_guard;

-- 2) Add unique key on generated guard column
SET @sql_add_unpaid_guard_uk = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'platform_payment'
        AND INDEX_NAME = 'uk_platform_payment_unpaid_guard'
    ),
    'SELECT 1',
    'ALTER TABLE platform_payment ADD UNIQUE KEY uk_platform_payment_unpaid_guard (unpaid_guard)'
  )
);
PREPARE stmt_add_unpaid_guard_uk FROM @sql_add_unpaid_guard_uk;
EXECUTE stmt_add_unpaid_guard_uk;
DEALLOCATE PREPARE stmt_add_unpaid_guard_uk;

-- 3) Add composite index for lock query pattern
SET @sql_add_order_status_id_idx = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'platform_payment'
        AND INDEX_NAME = 'idx_platform_payment_order_status_id'
    ),
    'SELECT 1',
    'ALTER TABLE platform_payment ADD KEY idx_platform_payment_order_status_id (order_id, status, id)'
  )
);
PREPARE stmt_add_order_status_id_idx FROM @sql_add_order_status_id_idx;
EXECUTE stmt_add_order_status_id_idx;
DEALLOCATE PREPARE stmt_add_order_status_id_idx;

