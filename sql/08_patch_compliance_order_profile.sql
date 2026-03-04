USE ai_link;

SET @db_name = DATABASE();

SET @sql_user_review_score = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'review_score'
    ),
    'SELECT 1',
    'ALTER TABLE user_account ADD COLUMN review_score DECIMAL(3,2) NOT NULL DEFAULT 5.00 AFTER worker_apply_status'
  )
);
PREPARE stmt_user_review_score FROM @sql_user_review_score;
EXECUTE stmt_user_review_score;
DEALLOCATE PREPARE stmt_user_review_score;

SET @sql_user_verify_status = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'verify_status'
    ),
    'SELECT 1',
    'ALTER TABLE user_account ADD COLUMN verify_status VARCHAR(20) NOT NULL DEFAULT ''UNVERIFIED'' AFTER review_score'
  )
);
PREPARE stmt_user_verify_status FROM @sql_user_verify_status;
EXECUTE stmt_user_verify_status;
DEALLOCATE PREPARE stmt_user_verify_status;

SET @sql_order_service_fee_rmb = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'service_fee_rmb'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD COLUMN service_fee_rmb BIGINT NOT NULL DEFAULT 0 AFTER service_fee_amount'
  )
);
PREPARE stmt_order_service_fee_rmb FROM @sql_order_service_fee_rmb;
EXECUTE stmt_order_service_fee_rmb;
DEALLOCATE PREPARE stmt_order_service_fee_rmb;

SET @sql_order_labor_budget_amount = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'labor_budget_amount'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD COLUMN labor_budget_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 AFTER service_fee_paid_time'
  )
);
PREPARE stmt_order_labor_budget_amount FROM @sql_order_labor_budget_amount;
EXECUTE stmt_order_labor_budget_amount;
DEALLOCATE PREPARE stmt_order_labor_budget_amount;

SET @sql_order_labor_budget_currency = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'labor_budget_currency'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD COLUMN labor_budget_currency VARCHAR(10) NOT NULL DEFAULT ''USD'' AFTER labor_budget_amount'
  )
);
PREPARE stmt_order_labor_budget_currency FROM @sql_order_labor_budget_currency;
EXECUTE stmt_order_labor_budget_currency;
DEALLOCATE PREPARE stmt_order_labor_budget_currency;

SET @sql_order_employer_declared_paid = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'employer_declared_paid'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD COLUMN employer_declared_paid TINYINT NOT NULL DEFAULT 0 AFTER labor_budget_currency'
  )
);
PREPARE stmt_order_employer_declared_paid FROM @sql_order_employer_declared_paid;
EXECUTE stmt_order_employer_declared_paid;
DEALLOCATE PREPARE stmt_order_employer_declared_paid;

SET @sql_order_employer_declared_paid_time = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'employer_declared_paid_time'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD COLUMN employer_declared_paid_time DATETIME NULL AFTER employer_declared_paid'
  )
);
PREPARE stmt_order_employer_declared_paid_time FROM @sql_order_employer_declared_paid_time;
EXECUTE stmt_order_employer_declared_paid_time;
DEALLOCATE PREPARE stmt_order_employer_declared_paid_time;

SET @sql_order_worker_confirmed_paid = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'worker_confirmed_paid'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD COLUMN worker_confirmed_paid TINYINT NOT NULL DEFAULT 0 AFTER employer_declared_paid_time'
  )
);
PREPARE stmt_order_worker_confirmed_paid FROM @sql_order_worker_confirmed_paid;
EXECUTE stmt_order_worker_confirmed_paid;
DEALLOCATE PREPARE stmt_order_worker_confirmed_paid;

SET @sql_order_worker_confirmed_paid_time = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'worker_confirmed_paid_time'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD COLUMN worker_confirmed_paid_time DATETIME NULL AFTER worker_confirmed_paid'
  )
);
PREPARE stmt_order_worker_confirmed_paid_time FROM @sql_order_worker_confirmed_paid_time;
EXECUTE stmt_order_worker_confirmed_paid_time;
DEALLOCATE PREPARE stmt_order_worker_confirmed_paid_time;

SET @sql_runner_wise_id = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'runner_payment_profile' AND COLUMN_NAME = 'wise_id'
    ),
    'SELECT 1',
    'ALTER TABLE runner_payment_profile ADD COLUMN wise_id VARCHAR(80) NULL AFTER paypal_email'
  )
);
PREPARE stmt_runner_wise_id FROM @sql_runner_wise_id;
EXECUTE stmt_runner_wise_id;
DEALLOCATE PREPARE stmt_runner_wise_id;

SET @sql_runner_payoneer_link = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'runner_payment_profile' AND COLUMN_NAME = 'payoneer_link'
    ),
    'SELECT 1',
    'ALTER TABLE runner_payment_profile ADD COLUMN payoneer_link VARCHAR(255) NULL AFTER wise_link'
  )
);
PREPARE stmt_runner_payoneer_link FROM @sql_runner_payoneer_link;
EXECUTE stmt_runner_payoneer_link;
DEALLOCATE PREPARE stmt_runner_payoneer_link;

SET @sql_runner_crypto_wallet = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'runner_payment_profile' AND COLUMN_NAME = 'crypto_wallet'
    ),
    'SELECT 1',
    'ALTER TABLE runner_payment_profile ADD COLUMN crypto_wallet VARCHAR(255) NULL AFTER payoneer_link'
  )
);
PREPARE stmt_runner_crypto_wallet FROM @sql_runner_crypto_wallet;
EXECUTE stmt_runner_crypto_wallet;
DEALLOCATE PREPARE stmt_runner_crypto_wallet;
