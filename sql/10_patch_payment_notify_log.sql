CREATE TABLE `payment_notify_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id` varchar(128) NOT NULL COMMENT '第三方回调事件ID',
  `payment_no` varchar(64) NOT NULL COMMENT '平台支付单号(我方流水号)',
  `transaction_id` varchar(128) DEFAULT NULL COMMENT '第三方支付系统交易单号',
  `notify_content` text COMMENT '回调原始报文JSON',
  `process_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态: PENDING/SUCCESS/FAIL/IGNORED',
  `error_msg` varchar(512) DEFAULT NULL COMMENT '处理失败时的异常信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(2) NOT NULL DEFAULT '0' COMMENT '逻辑删除标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_payment_no` (`payment_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调通知记录表';
