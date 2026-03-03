INSERT INTO user_account (id, role, username, password, email, country, city, status, worker_apply_status, deleted)
VALUES
  (1, 'EMPLOYER', 'client_a', 'x', 'client@ailink.test', 'CN', 'Shanghai', 1, 'NONE', 0),
  (2, 'WORKER', 'runner_a', 'x', 'runner@ailink.test', 'SG', 'Singapore', 1, 'APPROVED', 0);

INSERT INTO demand (id, user_id, target_country, category, budget, description, status, deleted)
VALUES
  (10, 1, 'SG', 'translation', 1000.00, 'test demand', 'OPEN', 0);

INSERT INTO worker_profile
  (id, user_id, country, city, skill_tags, price_min, price_max, experience, rating, verified, real_name, id_no_hash, deleted)
VALUES
  (20, 2, 'SG', 'Singapore', 'translation,copywriting', 100.00, 300.00, '5 years', 5.0, 1, 'Runner A', 'hash', 0);

INSERT INTO platform_config (config_key, config_value, description, deleted)
VALUES
  ('platform_fee_rate', '0.06', 'platform fee', 0),
  ('service_fee_rate', '0.10', 'service fee', 0),
  ('escrow_fee_rate', '0.005', 'escrow fee', 0);
