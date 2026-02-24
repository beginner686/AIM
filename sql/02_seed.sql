USE ai_link;

INSERT INTO platform_config (config_key, config_value, description)
VALUES
  ('platform_fee_rate', '0.06', 'platform commission rate'),
  ('escrow_fee_rate', '0.005', 'escrow handling fee rate')
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  description = VALUES(description),
  updated_time = CURRENT_TIMESTAMP;

-- Default admin account:
-- username: admin
-- password: password
INSERT INTO user_account (role, username, password, email, country, city, status)
VALUES
  ('ADMIN', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@ailink.com', 'CN', 'Shanghai', 1)
ON DUPLICATE KEY UPDATE
  role = VALUES(role),
  status = VALUES(status),
  updated_time = CURRENT_TIMESTAMP;
