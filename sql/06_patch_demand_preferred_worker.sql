USE ai_link;

ALTER TABLE demand
  ADD COLUMN IF NOT EXISTS preferred_worker_profile_id BIGINT NULL AFTER user_id,
  ADD COLUMN IF NOT EXISTS preferred_worker_user_id BIGINT NULL AFTER preferred_worker_profile_id,
  ADD COLUMN IF NOT EXISTS preferred_worker_name_snapshot VARCHAR(100) NULL AFTER preferred_worker_user_id;
