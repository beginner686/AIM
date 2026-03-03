USE ai_link;

ALTER TABLE user_account
  ADD COLUMN IF NOT EXISTS worker_apply_status VARCHAR(20) NOT NULL DEFAULT 'NONE' AFTER status;

CREATE INDEX idx_user_worker_apply_status ON user_account(worker_apply_status);

CREATE TABLE IF NOT EXISTS worker_apply (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  country VARCHAR(100) NOT NULL,
  city VARCHAR(100) NOT NULL,
  skill_tags TEXT NOT NULL,
  price_min DECIMAL(12,2) DEFAULT 0.00,
  price_max DECIMAL(12,2) DEFAULT 0.00,
  experience TEXT,
  real_name VARCHAR(100) NOT NULL,
  id_no_hash VARCHAR(128) NOT NULL,
  apply_note VARCHAR(500),
  review_note VARCHAR(500),
  reviewed_by BIGINT,
  reviewed_time DATETIME,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_worker_apply_user (user_id),
  KEY idx_worker_apply_status (status),
  KEY idx_worker_apply_created (created_time),
  CONSTRAINT fk_worker_apply_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_worker_apply_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE user_account
SET worker_apply_status = 'APPROVED'
WHERE role = 'WORKER'
  AND (worker_apply_status IS NULL OR worker_apply_status <> 'APPROVED');
