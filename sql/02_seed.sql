USE ai_link;

INSERT INTO platform_config (config_key, config_value, description)
VALUES
  ('platform_fee_rate', '0.06', 'platform commission rate'),
  ('escrow_fee_rate', '0.005', 'escrow handling fee rate'),
  ('service_fee_rate', '0.10', 'service fee rate for unlocking contact')
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

-- Disable legacy COUNTRY entries (non-ISO codes) to avoid duplicate/mixed-language options.
UPDATE dict_item
SET enabled = 0,
    updated_time = CURRENT_TIMESTAMP
WHERE dict_type = 'COUNTRY'
  AND dict_code IN ('Singapore', 'Japan', 'UAE', 'Germany', 'Australia')
  AND deleted = 0;

INSERT INTO dict_item (dict_type, dict_code, dict_label, tag_type, group_key, sort_no, enabled, extra_json)
VALUES
  ('CATEGORY', 'website_development', '网站开发', NULL, NULL, 10, 1, '{"hot":true}'),
  ('CATEGORY', 'ui_design', 'UI设计', NULL, NULL, 20, 1, '{"hot":true}'),
  ('CATEGORY', 'translation', '翻译', NULL, NULL, 30, 1, '{"hot":true}'),
  ('CATEGORY', 'video_editing', '视频剪辑', NULL, NULL, 40, 1, '{"hot":true}'),
  ('CATEGORY', 'ai_service', 'AI服务', NULL, NULL, 50, 1, '{"hot":true}'),
  ('CATEGORY', 'remote_assistant', '远程助理', NULL, NULL, 60, 1, '{"hot":false}'),
  ('CATEGORY', 'overseas_ads', '海外投放', NULL, NULL, 70, 1, '{"hot":false}'),
  ('CATEGORY', 'customer_support', '客服支持', NULL, NULL, 80, 1, '{"hot":false}'),
  ('CATEGORY', 'graphic_design', '平面设计', NULL, NULL, 60, 1, NULL),
  ('CATEGORY', 'ui_ux_design', 'UI/UX设计', NULL, NULL, 70, 1, NULL),
  ('CATEGORY', 'logo_brand_design', 'Logo/品牌设计', NULL, NULL, 80, 1, NULL),
  ('CATEGORY', 'copywriting', '文案策划', NULL, NULL, 90, 1, NULL),
  ('CATEGORY', 'social_media_ops', '社媒运营', NULL, NULL, 100, 1, NULL),
  ('CATEGORY', 'seo_optimization', 'SEO优化', NULL, NULL, 110, 1, NULL),
  ('CATEGORY', 'sem_bidding', 'SEM竞价托管', NULL, NULL, 120, 1, NULL),
  ('CATEGORY', 'influencer_marketing', '网红/KOL合作', NULL, NULL, 130, 1, NULL),
  ('CATEGORY', 'content_marketing', '内容营销', NULL, NULL, 140, 1, NULL),
  ('CATEGORY', 'email_marketing', '邮件营销', NULL, NULL, 150, 1, NULL),
  ('CATEGORY', 'web_development', '网站开发', NULL, NULL, 160, 1, NULL),
  ('CATEGORY', 'app_development', 'App开发', NULL, NULL, 170, 1, NULL),
  ('CATEGORY', 'mini_program_dev', '小程序开发', NULL, NULL, 180, 1, NULL),
  ('CATEGORY', 'data_analysis', '数据分析', NULL, NULL, 190, 1, NULL),
  ('CATEGORY', 'market_research', '市场调研', NULL, NULL, 200, 1, NULL),
  ('CATEGORY', 'ecommerce_ops', '电商代运营', NULL, NULL, 210, 1, NULL),
  ('CATEGORY', 'product_listing', '产品上架', NULL, NULL, 220, 1, NULL),
  ('CATEGORY', 'cross_border_logistics', '跨境物流', NULL, NULL, 230, 1, NULL),
  ('CATEGORY', 'overseas_warehousing', '海外仓储', NULL, NULL, 240, 1, NULL),
  ('CATEGORY', 'customs_clearance', '报关清关', NULL, NULL, 250, 1, NULL),
  ('CATEGORY', 'finance_tax', '财税服务', NULL, NULL, 260, 1, NULL),
  ('CATEGORY', 'overseas_company_reg', '海外公司注册', NULL, NULL, 270, 1, NULL),
  ('CATEGORY', 'legal_consulting', '法律咨询', NULL, NULL, 280, 1, NULL),
  ('CATEGORY', 'contract_drafting', '合同起草', NULL, NULL, 290, 1, NULL),
  ('CATEGORY', 'ip_trademark', '知识产权/商标', NULL, NULL, 300, 1, NULL),
  ('CATEGORY', 'overseas_recruitment', '海外招聘', NULL, NULL, 310, 1, NULL),
  ('CATEGORY', 'hr_outsourcing', '人事外包', NULL, NULL, 320, 1, NULL),
  ('CATEGORY', 'voiceover_dubbing', '配音解说', NULL, NULL, 330, 1, NULL),
  ('CATEGORY', 'photography', '商业摄影', NULL, NULL, 340, 1, NULL),
  ('CATEGORY', 'animation_motion', '动画制作', NULL, NULL, 350, 1, NULL),
  ('CATEGORY', 'live_streaming', '直播运营', NULL, NULL, 360, 1, NULL),
  ('CATEGORY', 'ppt_presentation', 'PPT/演示设计', NULL, NULL, 370, 1, NULL),
  ('CATEGORY', 'ai_training_data', 'AI数据标注', NULL, NULL, 380, 1, NULL),
  ('CATEGORY', 'certification_audit', '认证审计', NULL, NULL, 390, 1, NULL),
  ('CATEGORY', 'other', '其他服务', NULL, NULL, 999, 1, NULL),

  ('COUNTRY', 'CN', '中国', NULL, NULL, 10, 1, '{"hot":true,"region":"ASIA"}'),
  ('COUNTRY', 'SG', '新加坡', NULL, NULL, 20, 1, '{"hot":true,"region":"ASIA"}'),
  ('COUNTRY', 'JP', '日本', NULL, NULL, 30, 1, '{"hot":false,"region":"ASIA"}'),
  ('COUNTRY', 'KR', '韩国', NULL, NULL, 40, 1, '{"hot":false,"region":"ASIA"}'),
  ('COUNTRY', 'US', '美国', NULL, NULL, 50, 1, '{"hot":true,"region":"OTHER"}'),
  ('COUNTRY', 'GB', '英国', NULL, NULL, 60, 1, '{"hot":false,"region":"EUROPE"}'),
  ('COUNTRY', 'AU', '澳大利亚', NULL, NULL, 70, 1, '{"hot":false,"region":"OTHER"}'),
  ('COUNTRY', 'AE', '阿联酋', NULL, NULL, 80, 1, '{"hot":false,"region":"ASIA"}'),

  ('USER_ROLE', 'ADMIN', '管理员', 'danger', NULL, 10, 1, NULL),
  ('USER_ROLE', 'USER', '用户', 'info', NULL, 20, 1, NULL),
  ('USER_ROLE', 'EMPLOYER', '雇主', 'warning', NULL, 30, 1, NULL),
  ('USER_ROLE', 'WORKER', '执行者', 'success', NULL, 40, 1, NULL),

  ('DEMAND_STATUS', 'OPEN', '开放中', 'success', 'OPEN', 10, 1, '{"groups":["OPEN"]}'),
  ('DEMAND_STATUS', 'MATCHED', '已匹配', 'warning', 'ACTIVE', 20, 1, '{"groups":["ACTIVE"]}'),
  ('DEMAND_STATUS', 'IN_PROGRESS', '进行中', 'warning', 'ACTIVE', 30, 1, '{"groups":["ACTIVE"]}'),
  ('DEMAND_STATUS', 'DONE', '已完成', 'info', 'DONE', 40, 1, '{"groups":["DONE"]}'),
  ('DEMAND_STATUS', 'CLOSED', '已关闭', 'danger', 'CLOSED', 50, 1, '{"groups":["CLOSED"]}'),

  ('ORDER_STATUS', 'CREATED', '已创建', 'info', 'ACTIVE', 10, 1, '{"groups":["ACTIVE"]}'),
  ('ORDER_STATUS', 'SERVICE_FEE_REQUIRED', '待支付服务费', 'warning', 'ACTIVE', 20, 1, '{"groups":["ACTIVE"]}'),
  ('ORDER_STATUS', 'SERVICE_FEE_PAID', '服务费已支付', 'info', 'ACTIVE', 30, 1, '{"groups":["ACTIVE"]}'),
  ('ORDER_STATUS', 'MATCH_UNLOCKED', '已解锁匹配', 'success', 'ACTIVE', 40, 1, '{"groups":["ACTIVE"]}'),
  ('ORDER_STATUS', 'IN_PROGRESS', '进行中', 'warning', 'ACTIVE', 50, 1, '{"groups":["ACTIVE"]}'),
  ('ORDER_STATUS', 'COMPLETED', '已完成', 'success', 'COMPLETED', 60, 1, '{"groups":["COMPLETED"]}'),
  ('ORDER_STATUS', 'DISPUTE', '争议中', 'danger', 'DISPUTE', 70, 1, '{"groups":["ACTIVE","DISPUTE"]}'),
  ('ORDER_STATUS', 'ARBITRATION', '仲裁中', 'danger', 'DISPUTE', 80, 1, '{"groups":["ACTIVE","DISPUTE"]}'),
  ('ORDER_STATUS', 'CLOSED', '已关闭', 'info', 'CLOSED', 90, 1, '{"groups":["CLOSED"]}'),

  ('PAY_STATUS', 'UNPAID', '待支付', 'info', NULL, 10, 1, NULL),
  ('PAY_STATUS', 'PAID', '已支付', 'warning', NULL, 20, 1, NULL),
  ('PAY_STATUS', 'RELEASED', '已释放', 'success', NULL, 30, 1, NULL),
  ('PAY_STATUS', 'REFUNDED', '已退款', 'danger', NULL, 40, 1, NULL)
ON DUPLICATE KEY UPDATE
  dict_label = VALUES(dict_label),
  tag_type = VALUES(tag_type),
  group_key = VALUES(group_key),
  sort_no = VALUES(sort_no),
  enabled = VALUES(enabled),
  extra_json = VALUES(extra_json),
  updated_time = CURRENT_TIMESTAMP;
