USE ai_link;

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

