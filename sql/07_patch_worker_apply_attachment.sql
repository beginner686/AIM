USE ai_link;

SET @db_name = DATABASE();

SET @sql_add_apply_attachment_name = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'worker_apply'
        AND COLUMN_NAME = 'apply_attachment_name'
    ),
    'SELECT 1',
    'ALTER TABLE worker_apply ADD COLUMN apply_attachment_name VARCHAR(200) NULL AFTER apply_note'
  )
);
PREPARE stmt_add_apply_attachment_name FROM @sql_add_apply_attachment_name;
EXECUTE stmt_add_apply_attachment_name;
DEALLOCATE PREPARE stmt_add_apply_attachment_name;

SET @sql_add_apply_attachment_url = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'worker_apply'
        AND COLUMN_NAME = 'apply_attachment_url'
    ),
    'SELECT 1',
    'ALTER TABLE worker_apply ADD COLUMN apply_attachment_url VARCHAR(500) NULL AFTER apply_attachment_name'
  )
);
PREPARE stmt_add_apply_attachment_url FROM @sql_add_apply_attachment_url;
EXECUTE stmt_add_apply_attachment_url;
DEALLOCATE PREPARE stmt_add_apply_attachment_url;
