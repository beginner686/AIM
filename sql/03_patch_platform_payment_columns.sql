USE ai_link;

-- Patch legacy platform_payment table to align with current code.
-- This script is idempotent and safe to run multiple times.

SET @db := DATABASE();

-- transaction_id
SET @has_tx_id := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'platform_payment'
    AND COLUMN_NAME = 'transaction_id'
);
SET @sql := IF(
  @has_tx_id = 0,
  'ALTER TABLE platform_payment ADD COLUMN transaction_id VARCHAR(64) NULL AFTER status',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- transaction_no
SET @has_tx_no := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'platform_payment'
    AND COLUMN_NAME = 'transaction_no'
);
SET @sql := IF(
  @has_tx_no = 0,
  'ALTER TABLE platform_payment ADD COLUMN transaction_no VARCHAR(64) NULL AFTER transaction_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- code_url
SET @has_code_url := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'platform_payment'
    AND COLUMN_NAME = 'code_url'
);
SET @sql := IF(
  @has_code_url = 0,
  'ALTER TABLE platform_payment ADD COLUMN code_url VARCHAR(1024) NULL AFTER transaction_no',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- unique index for transaction_id (for notify idempotency)
SET @has_tx_id_idx := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'platform_payment'
    AND INDEX_NAME = 'uk_platform_payment_tx_id'
);
SET @sql := IF(
  @has_tx_id_idx = 0,
  'ALTER TABLE platform_payment ADD UNIQUE KEY uk_platform_payment_tx_id (transaction_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
