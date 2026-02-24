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
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_email (email),
  KEY idx_user_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS demand (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
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

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  demand_id BIGINT NOT NULL,
  employer_id BIGINT NOT NULL,
  worker_profile_id BIGINT NOT NULL,
  worker_user_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
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
  KEY idx_order_created_time (created_time)
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
