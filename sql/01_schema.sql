CREATE DATABASE IF NOT EXISTS ai_link
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_link;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role VARCHAR(20) NOT NULL,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(100) NOT NULL,
  country VARCHAR(100),
  city VARCHAR(100),
  status TINYINT NOT NULL DEFAULT 1,
  worker_apply_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_email (email),
  KEY idx_user_role_status (role, status),
  KEY idx_user_worker_apply_status (worker_apply_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS demand (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  preferred_worker_profile_id BIGINT,
  preferred_worker_user_id BIGINT,
  preferred_worker_name_snapshot VARCHAR(100),
  target_country VARCHAR(100) NOT NULL,
  category VARCHAR(100) NOT NULL,
  budget DECIMAL(12,2) NOT NULL,
  deadline DATETIME,
  description TEXT NOT NULL,
  ai_structured JSON,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_demand_user (user_id),
  KEY idx_demand_pref_worker_profile (preferred_worker_profile_id),
  KEY idx_demand_pref_worker_user (preferred_worker_user_id),
  KEY idx_demand_country (target_country),
  KEY idx_demand_category (category),
  KEY idx_demand_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS worker_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  country VARCHAR(100) NOT NULL,
  city VARCHAR(100) NOT NULL,
  skill_tags TEXT NOT NULL,
  price_min DECIMAL(12,2) DEFAULT 0.00,
  price_max DECIMAL(12,2) DEFAULT 0.00,
  experience TEXT,
  rating FLOAT NOT NULL DEFAULT 5.0,
  verified TINYINT NOT NULL DEFAULT 0,
  real_name VARCHAR(100) NOT NULL,
  id_no_hash VARCHAR(128) NOT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_worker_user (user_id),
  KEY idx_worker_country_city (country, city),
  KEY idx_worker_verified (verified)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  demand_id BIGINT NOT NULL,
  employer_id BIGINT NOT NULL,
  worker_profile_id BIGINT NOT NULL,
  worker_user_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  service_fee_status VARCHAR(20) NOT NULL DEFAULT 'REQUIRED',
  service_fee_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  service_fee_paid_time DATETIME,
  pay_status VARCHAR(20) NOT NULL,
  payment_channel VARCHAR(30) NOT NULL,
  platform_fee_rate DECIMAL(6,4) NOT NULL,
  platform_fee DECIMAL(12,2) NOT NULL,
  escrow_fee_rate DECIMAL(6,4) NOT NULL,
  escrow_fee DECIMAL(12,2) NOT NULL,
  worker_income DECIMAL(12,2) NOT NULL,
  risk_status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  dispute_reason VARCHAR(500),
  closed_reason VARCHAR(500),
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_order_demand (demand_id),
  KEY idx_order_employer (employer_id),
  KEY idx_order_worker_user (worker_user_id),
  KEY idx_order_status (status),
  KEY idx_order_created_time (created_time),
  UNIQUE KEY uk_order_no (order_no),
  CONSTRAINT fk_order_demand FOREIGN KEY (demand_id) REFERENCES demand(id),
  CONSTRAINT fk_order_worker_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profile(id),
  CONSTRAINT fk_order_employer FOREIGN KEY (employer_id) REFERENCES user_account(id),
  CONSTRAINT fk_order_worker_user FOREIGN KEY (worker_user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS demand_apply (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  demand_id BIGINT NOT NULL,
  demand_owner_id BIGINT NOT NULL,
  worker_user_id BIGINT NOT NULL,
  worker_profile_id BIGINT NOT NULL,
  quote_amount DECIMAL(12,2) NOT NULL,
  apply_note VARCHAR(1000),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  review_note VARCHAR(500),
  order_id BIGINT,
  handled_time DATETIME,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_demand_apply_demand (demand_id),
  KEY idx_demand_apply_owner (demand_owner_id),
  KEY idx_demand_apply_worker_user (worker_user_id),
  KEY idx_demand_apply_status (status),
  UNIQUE KEY uk_demand_apply_demand_worker (demand_id, worker_user_id, deleted),
  CONSTRAINT fk_demand_apply_demand FOREIGN KEY (demand_id) REFERENCES demand(id),
  CONSTRAINT fk_demand_apply_owner FOREIGN KEY (demand_owner_id) REFERENCES user_account(id),
  CONSTRAINT fk_demand_apply_worker_user FOREIGN KEY (worker_user_id) REFERENCES user_account(id),
  CONSTRAINT fk_demand_apply_worker_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profile(id),
  CONSTRAINT fk_demand_apply_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(20),
  to_status VARCHAR(20) NOT NULL,
  operator_id BIGINT NOT NULL,
  remark VARCHAR(255),
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_order_log_order (order_id),
  KEY idx_order_log_created (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS escrow_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  channel VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  transaction_no VARCHAR(64),
  released_time DATETIME,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_escrow_order (order_id),
  KEY idx_escrow_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS platform_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(50) NOT NULL,
  config_value VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_platform_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dict_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dict_type VARCHAR(50) NOT NULL,
  dict_code VARCHAR(80) NOT NULL,
  dict_label VARCHAR(120) NOT NULL,
  tag_type VARCHAR(20) DEFAULT NULL,
  group_key VARCHAR(50) DEFAULT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  extra_json VARCHAR(1000) DEFAULT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_dict_type_code (dict_type, dict_code),
  KEY idx_dict_type_enabled_sort (dict_type, enabled, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS runner_payment_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  paypal_email VARCHAR(120),
  wise_link VARCHAR(255),
  payment_url VARCHAR(255),
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
  verified TINYINT NOT NULL DEFAULT 0,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_runner_pay_user (user_id),
  KEY idx_runner_pay_verified (verified),
  CONSTRAINT fk_runner_pay_user FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS platform_payment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  payment_no VARCHAR(40) NOT NULL,
  order_id BIGINT NOT NULL,
  payer_id BIGINT NOT NULL,
  payment_channel VARCHAR(20) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  transaction_id VARCHAR(64),
  transaction_no VARCHAR(64),
  code_url VARCHAR(1024),
  paid_time DATETIME,
  remark VARCHAR(255),
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_platform_payment_no (payment_no),
  UNIQUE KEY uk_platform_payment_tx_id (transaction_id),
  KEY idx_platform_payment_order (order_id),
  KEY idx_platform_payment_status (status),
  CONSTRAINT fk_platform_payment_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_platform_payment_user FOREIGN KEY (payer_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dispute_ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  creator_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  evidence_url VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  resolver_id BIGINT,
  resolution VARCHAR(500),
  resolved_time DATETIME,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_dispute_order (order_id),
  KEY idx_dispute_status (status),
  CONSTRAINT fk_dispute_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_dispute_creator FOREIGN KEY (creator_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  warning TINYINT NOT NULL DEFAULT 0,
  warning_message VARCHAR(255),
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_chat_order_time (order_id, created_time),
  KEY idx_chat_sender (sender_id),
  CONSTRAINT fk_chat_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  reviewee_id BIGINT NOT NULL,
  score INT NOT NULL,
  content VARCHAR(1000),
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_reviewer (order_id, reviewer_id),
  KEY idx_review_reviewee (reviewee_id),
  CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES user_account(id),
  CONSTRAINT fk_review_reviewee FOREIGN KEY (reviewee_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
