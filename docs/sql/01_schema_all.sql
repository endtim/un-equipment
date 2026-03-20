-- =========================================================
-- Integrated schema SQL
-- File: 01_schema_all.sql
-- Execute: run this file directly
-- Includes:
--   1) un-equipment.sql
--   2) upgrade_20260310_instrument_fields.sql
--   3) upgrade_20260313_open_rule_multi_days.sql
-- =========================================================

-- 高校大型仪器共享平台 MySQL 建表脚本 V1
-- 建议环境：MySQL 8.x
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_unicode_ci

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS instrument_share_platform
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE instrument_share_platform;

-- =========================
-- 1. 基础表
-- =========================

DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS sys_message;
DROP TABLE IF EXISTS content_help_doc;
DROP TABLE IF EXISTS content_notice;
DROP TABLE IF EXISTS stat_daily_snapshot;
DROP TABLE IF EXISTS biz_settlement_record;
DROP TABLE IF EXISTS biz_transaction_record;
DROP TABLE IF EXISTS biz_recharge_order;
DROP TABLE IF EXISTS biz_account;
DROP TABLE IF EXISTS biz_result_file;
DROP TABLE IF EXISTS biz_sample_order;
DROP TABLE IF EXISTS biz_usage_record;
DROP TABLE IF EXISTS biz_reservation_audit;
DROP TABLE IF EXISTS biz_reservation_order;
DROP TABLE IF EXISTS biz_instrument_close_period;
DROP TABLE IF EXISTS biz_instrument_open_rule;
DROP TABLE IF EXISTS biz_instrument_attachment;
DROP TABLE IF EXISTS biz_maintenance_record;
DROP TABLE IF EXISTS biz_instrument;
DROP TABLE IF EXISTS biz_instrument_category;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_department;

CREATE TABLE sys_department (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  parent_id BIGINT DEFAULT NULL COMMENT '上级部门ID',
  dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
  dept_code VARCHAR(50) DEFAULT NULL COMMENT '部门编码',
  leader_user_id BIGINT DEFAULT NULL COMMENT '部门负责人ID',
  phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_department_dept_code (dept_code),
  KEY idx_sys_department_parent_id (parent_id),
  KEY idx_sys_department_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

CREATE TABLE sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
  role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(50) NOT NULL COMMENT '用户名',
  password VARCHAR(100) NOT NULL COMMENT '密码',
  real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
  user_type VARCHAR(20) NOT NULL COMMENT '用户类型 INTERNAL/EXTERNAL',
  user_no VARCHAR(50) DEFAULT NULL COMMENT '工号/学号/外部编号',
  gender VARCHAR(10) DEFAULT NULL COMMENT '性别',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  auth_type VARCHAR(20) NOT NULL DEFAULT 'LOCAL' COMMENT '认证方式 LOCAL/SSO/WECHAT',
  department_id BIGINT DEFAULT NULL COMMENT '所属部门ID',
  unit_name VARCHAR(100) DEFAULT NULL COMMENT '校外用户单位',
  title_name VARCHAR(50) DEFAULT NULL COMMENT '职称/身份',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_department_id (department_id),
  KEY idx_sys_user_user_type (user_type),
  KEY idx_sys_user_status (status),
  CONSTRAINT fk_sys_user_department_id FOREIGN KEY (department_id) REFERENCES sys_department (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE sys_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_role_user_id_role_id (user_id, role_id),
  KEY idx_sys_user_role_role_id (role_id),
  CONSTRAINT fk_sys_user_role_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_sys_user_role_role_id FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- =========================
-- 2. 仪器相关表
-- =========================

CREATE TABLE biz_instrument_category (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  parent_id BIGINT DEFAULT NULL COMMENT '父级分类ID',
  category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
  category_code VARCHAR(50) DEFAULT NULL COMMENT '分类编码',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_instrument_category_code (category_code),
  KEY idx_biz_instrument_category_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仪器分类表';

CREATE TABLE biz_instrument (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  instrument_no VARCHAR(50) NOT NULL COMMENT '仪器编号',
  instrument_name VARCHAR(200) NOT NULL COMMENT '仪器名称',
  model VARCHAR(100) DEFAULT NULL COMMENT '型号',
  brand VARCHAR(100) DEFAULT NULL COMMENT '品牌',
  asset_no VARCHAR(50) DEFAULT NULL COMMENT '资产编号',
  manufacturer VARCHAR(100) DEFAULT NULL COMMENT '生产厂家',
  supplier VARCHAR(100) DEFAULT NULL COMMENT '供应商',
  origin_country VARCHAR(50) DEFAULT NULL COMMENT '制造国家',
  purchase_date DATE DEFAULT NULL COMMENT '购置日期',
  production_date DATE DEFAULT NULL COMMENT '生产日期',
  equipment_source VARCHAR(50) DEFAULT NULL COMMENT '设备来源',
  service_contact_name VARCHAR(50) DEFAULT NULL COMMENT '使用联系人',
  service_contact_phone VARCHAR(20) DEFAULT NULL COMMENT '联系人电话',
  category_id BIGINT NOT NULL COMMENT '分类ID',
  department_id BIGINT NOT NULL COMMENT '所属部门ID',
  owner_user_id BIGINT NOT NULL COMMENT '仪器负责人ID',
  location VARCHAR(200) DEFAULT NULL COMMENT '放置地点',
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '仪器状态 NORMAL/IN_USE/STANDBY/FAULT/MAINTENANCE/DISABLED',
  open_mode VARCHAR(20) NOT NULL COMMENT '开放模式 MACHINE/SAMPLE/BOTH',
  open_status TINYINT NOT NULL DEFAULT 1 COMMENT '开放状态 1开放0关闭',
  support_external TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持校外服务',
  need_audit TINYINT NOT NULL DEFAULT 1 COMMENT '是否需要审核',
  require_training TINYINT NOT NULL DEFAULT 0 COMMENT '是否要求培训',
  booking_unit VARCHAR(20) DEFAULT NULL COMMENT '计费单位 HOUR/SAMPLE/PROJECT',
  price_internal DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '校内价格',
  price_external DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '校外价格',
  min_reserve_minutes INT NOT NULL DEFAULT 30 COMMENT '最小预约时长(分钟)',
  max_reserve_minutes INT NOT NULL DEFAULT 480 COMMENT '最大预约时长(分钟)',
  step_minutes INT NOT NULL DEFAULT 30 COMMENT '时间步长(分钟)',
  cover_url VARCHAR(255) DEFAULT NULL COMMENT '封面图',
  intro TEXT DEFAULT NULL COMMENT '仪器简介',
  usage_desc TEXT DEFAULT NULL COMMENT '使用说明',
  sample_desc TEXT DEFAULT NULL COMMENT '送样说明',
  notice_text TEXT DEFAULT NULL COMMENT '特别说明',
  technical_specs TEXT DEFAULT NULL COMMENT '主要技术指标',
  main_functions TEXT DEFAULT NULL COMMENT '主要功能',
  service_content TEXT DEFAULT NULL COMMENT '服务内容',
  user_notice TEXT DEFAULT NULL COMMENT '用户须知',
  charge_standard TEXT DEFAULT NULL COMMENT '收费标准说明',
  is_hot TINYINT NOT NULL DEFAULT 0 COMMENT '是否热门',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_instrument_instrument_no (instrument_no),
  KEY idx_biz_instrument_category_id (category_id),
  KEY idx_biz_instrument_department_id (department_id),
  KEY idx_biz_instrument_owner_user_id (owner_user_id),
  KEY idx_biz_instrument_status_open_status (status, open_status),
  CONSTRAINT fk_biz_instrument_category_id FOREIGN KEY (category_id) REFERENCES biz_instrument_category (id),
  CONSTRAINT fk_biz_instrument_department_id FOREIGN KEY (department_id) REFERENCES sys_department (id),
  CONSTRAINT fk_biz_instrument_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仪器主表';

CREATE TABLE biz_instrument_attachment (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  instrument_id BIGINT NOT NULL COMMENT '仪器ID',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名称',
  file_url VARCHAR(255) NOT NULL COMMENT '文件地址',
  file_type VARCHAR(20) NOT NULL COMMENT '文件类型 IMAGE/MANUAL/TEMPLATE/OTHER',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  KEY idx_biz_instrument_attachment_instrument_id (instrument_id),
  CONSTRAINT fk_biz_instrument_attachment_instrument_id FOREIGN KEY (instrument_id) REFERENCES biz_instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仪器附件表';

CREATE TABLE biz_instrument_open_rule (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  instrument_id BIGINT NOT NULL COMMENT '仪器ID',
  week_day TINYINT NOT NULL COMMENT '星期几 1-7',
  start_time TIME NOT NULL COMMENT '开始时间',
  end_time TIME NOT NULL COMMENT '结束时间',
  max_reserve_minutes INT NOT NULL DEFAULT 480 COMMENT '最大预约时长(分钟)',
  step_minutes INT NOT NULL DEFAULT 30 COMMENT '步长(分钟)',
  effective_start_date DATE DEFAULT NULL COMMENT '生效开始日期',
  effective_end_date DATE DEFAULT NULL COMMENT '生效结束日期',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  KEY idx_biz_instrument_open_rule_instrument_id (instrument_id),
  CONSTRAINT fk_biz_instrument_open_rule_instrument_id FOREIGN KEY (instrument_id) REFERENCES biz_instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仪器开放规则表';

CREATE TABLE biz_instrument_close_period (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  instrument_id BIGINT NOT NULL COMMENT '仪器ID',
  close_start DATETIME NOT NULL COMMENT '关闭开始时间',
  close_end DATETIME NOT NULL COMMENT '关闭结束时间',
  close_type VARCHAR(20) NOT NULL COMMENT '关闭类型 FAULT/MAINTENANCE/HOLIDAY/TEMP',
  reason VARCHAR(255) DEFAULT NULL COMMENT '关闭原因',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_biz_instrument_close_period_instrument_id (instrument_id),
  KEY idx_biz_instrument_close_period_close_start_close_end (close_start, close_end),
  CONSTRAINT fk_biz_instrument_close_period_instrument_id FOREIGN KEY (instrument_id) REFERENCES biz_instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仪器关闭时段表';

CREATE TABLE biz_maintenance_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  instrument_id BIGINT NOT NULL COMMENT '仪器ID',
  maint_type VARCHAR(20) NOT NULL COMMENT '维护类型 FAULT/REPAIR/MAINTENANCE/CALIBRATION',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  content TEXT DEFAULT NULL COMMENT '内容',
  start_time DATETIME DEFAULT NULL COMMENT '开始时间',
  end_time DATETIME DEFAULT NULL COMMENT '结束时间',
  operator_user_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/PROCESSING/FINISHED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_biz_maintenance_record_instrument_id (instrument_id),
  CONSTRAINT fk_biz_maintenance_record_instrument_id FOREIGN KEY (instrument_id) REFERENCES biz_instrument (id),
  CONSTRAINT fk_biz_maintenance_record_operator_user_id FOREIGN KEY (operator_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仪器维护记录表';

-- =========================
-- 3. 订单与业务表
-- =========================

CREATE TABLE biz_reservation_order (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  order_no VARCHAR(50) NOT NULL COMMENT '订单编号',
  order_type VARCHAR(20) NOT NULL COMMENT '订单类型 MACHINE/SAMPLE',
  user_id BIGINT NOT NULL COMMENT '下单用户ID',
  instrument_id BIGINT NOT NULL COMMENT '仪器ID',
  department_id BIGINT DEFAULT NULL COMMENT '下单用户所属部门ID',
  owner_user_id BIGINT NOT NULL COMMENT '仪器负责人ID',
  contact_name VARCHAR(50) NOT NULL COMMENT '联系人姓名',
  contact_phone VARCHAR(20) NOT NULL COMMENT '联系人手机号',
  purpose VARCHAR(255) DEFAULT NULL COMMENT '用途',
  project_name VARCHAR(100) DEFAULT NULL COMMENT '项目名称',
  reserve_start DATETIME DEFAULT NULL COMMENT '预约开始时间',
  reserve_end DATETIME DEFAULT NULL COMMENT '预约结束时间',
  reserve_minutes INT NOT NULL DEFAULT 0 COMMENT '预约时长(分钟)',
  order_status VARCHAR(30) NOT NULL COMMENT '订单状态',
  audit_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态 PENDING/PASS/REJECT',
  pay_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态 UNPAID/PENDING/PAID/REFUNDED',
  settlement_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '结算状态 PENDING/CONFIRMED/VOID/REFUNDED',
  estimated_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预估金额',
  final_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最终金额',
  source VARCHAR(20) NOT NULL DEFAULT 'WEB' COMMENT '来源 WEB/MOBILE',
  submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  approve_time DATETIME DEFAULT NULL COMMENT '审核通过时间',
  finish_time DATETIME DEFAULT NULL COMMENT '完成时间',
  cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_reservation_order_order_no (order_no),
  KEY idx_biz_reservation_order_user_id_order_status_create_time (user_id, order_status, create_time),
  KEY idx_biz_res_ord_inst_time_st (instrument_id, reserve_start, reserve_end, order_status),
  KEY idx_biz_reservation_order_owner_user_id_order_status_submit_time (owner_user_id, order_status, submit_time),
  KEY idx_biz_reservation_order_department_id (department_id),
  CONSTRAINT fk_biz_reservation_order_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_biz_reservation_order_instrument_id FOREIGN KEY (instrument_id) REFERENCES biz_instrument (id),
  CONSTRAINT fk_biz_reservation_order_department_id FOREIGN KEY (department_id) REFERENCES sys_department (id),
  CONSTRAINT fk_biz_reservation_order_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约主订单表';

CREATE TABLE biz_reservation_audit (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  node_no INT NOT NULL DEFAULT 1 COMMENT '审批节点序号',
  auditor_id BIGINT NOT NULL COMMENT '审核人ID',
  auditor_role VARCHAR(50) NOT NULL COMMENT '审核人角色编码',
  audit_result VARCHAR(20) NOT NULL COMMENT '审核结果 PASS/REJECT/RETURN',
  audit_opinion VARCHAR(255) DEFAULT NULL COMMENT '审核意见',
  audit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_biz_reservation_audit_order_id (order_id),
  KEY idx_biz_reservation_audit_auditor_id (auditor_id),
  CONSTRAINT fk_biz_reservation_audit_order_id FOREIGN KEY (order_id) REFERENCES biz_reservation_order (id),
  CONSTRAINT fk_biz_reservation_audit_auditor_id FOREIGN KEY (auditor_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约审核记录表';

CREATE TABLE biz_usage_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  instrument_id BIGINT NOT NULL COMMENT '仪器ID',
  operator_user_id BIGINT NOT NULL COMMENT '使用人ID',
  checkin_time DATETIME DEFAULT NULL COMMENT '签到时间',
  start_time DATETIME DEFAULT NULL COMMENT '开始使用时间',
  end_time DATETIME DEFAULT NULL COMMENT '结束使用时间',
  actual_minutes INT NOT NULL DEFAULT 0 COMMENT '实际使用时长(分钟)',
  abnormal_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否异常 0否1是',
  abnormal_desc VARCHAR(255) DEFAULT NULL COMMENT '异常说明',
  owner_confirm_user_id BIGINT DEFAULT NULL COMMENT '负责人确认人ID',
  owner_confirm_time DATETIME DEFAULT NULL COMMENT '负责人确认时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_usage_record_order_id (order_id),
  KEY idx_biz_usage_record_instrument_id (instrument_id),
  CONSTRAINT fk_biz_usage_record_order_id FOREIGN KEY (order_id) REFERENCES biz_reservation_order (id),
  CONSTRAINT fk_biz_usage_record_instrument_id FOREIGN KEY (instrument_id) REFERENCES biz_instrument (id),
  CONSTRAINT fk_biz_usage_record_operator_user_id FOREIGN KEY (operator_user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_biz_usage_record_owner_confirm_user_id FOREIGN KEY (owner_confirm_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上机使用记录表';

CREATE TABLE biz_sample_order (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  sample_name VARCHAR(100) NOT NULL COMMENT '样品名称',
  sample_count INT NOT NULL DEFAULT 1 COMMENT '样品数量',
  sample_type VARCHAR(50) DEFAULT NULL COMMENT '样品类型',
  sample_spec VARCHAR(100) DEFAULT NULL COMMENT '样品规格',
  test_requirement TEXT DEFAULT NULL COMMENT '测试要求',
  danger_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否危险样品 0否1是',
  danger_desc VARCHAR(255) DEFAULT NULL COMMENT '危险说明',
  receive_status VARCHAR(20) NOT NULL DEFAULT 'WAITING_RECEIVE' COMMENT '接样状态',
  received_time DATETIME DEFAULT NULL COMMENT '接样时间',
  receiver_user_id BIGINT DEFAULT NULL COMMENT '接样人ID',
  testing_status VARCHAR(20) NOT NULL DEFAULT 'WAITING_RECEIVE' COMMENT '检测状态',
  result_summary TEXT DEFAULT NULL COMMENT '结果摘要',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_sample_order_order_id (order_id),
  CONSTRAINT fk_biz_sample_order_order_id FOREIGN KEY (order_id) REFERENCES biz_reservation_order (id),
  CONSTRAINT fk_biz_sample_order_receiver_user_id FOREIGN KEY (receiver_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='送样订单附表';

CREATE TABLE biz_result_file (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名称',
  file_url VARCHAR(255) NOT NULL COMMENT '文件地址',
  file_type VARCHAR(20) NOT NULL COMMENT '文件类型 RESULT/RAW_DATA/IMAGE/CONTRACT/SETTLEMENT',
  upload_user_id BIGINT NOT NULL COMMENT '上传人ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  KEY idx_biz_result_file_order_id (order_id),
  CONSTRAINT fk_biz_result_file_order_id FOREIGN KEY (order_id) REFERENCES biz_reservation_order (id),
  CONSTRAINT fk_biz_result_file_upload_user_id FOREIGN KEY (upload_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结果文件表';

-- =========================
-- 4. 财务与结算表
-- =========================

CREATE TABLE biz_account (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  balance DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '余额',
  frozen_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
  total_recharge DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计充值',
  total_consume DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_account_user_id (user_id),
  CONSTRAINT fk_biz_account_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户表';

CREATE TABLE biz_recharge_order (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  recharge_no VARCHAR(50) NOT NULL COMMENT '充值单号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  amount DECIMAL(10,2) NOT NULL COMMENT '充值金额',
  pay_method VARCHAR(20) NOT NULL COMMENT '支付方式 OFFLINE/TRANSFER/INTERNAL',
  voucher_url VARCHAR(255) DEFAULT NULL COMMENT '凭证地址',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/PASS/REJECT/ARRIVED',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  audit_user_id BIGINT DEFAULT NULL COMMENT '审核人ID',
  audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_recharge_order_recharge_no (recharge_no),
  KEY idx_biz_recharge_order_user_id_status_create_time (user_id, status, create_time),
  CONSTRAINT fk_biz_recharge_order_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_biz_recharge_order_audit_user_id FOREIGN KEY (audit_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值申请表';

CREATE TABLE biz_transaction_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  txn_no VARCHAR(50) NOT NULL COMMENT '流水号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  order_id BIGINT DEFAULT NULL COMMENT '关联订单ID',
  recharge_id BIGINT DEFAULT NULL COMMENT '关联充值ID',
  txn_type VARCHAR(20) NOT NULL COMMENT '流水类型 RECHARGE/CONSUME/REFUND/ADJUST',
  inout_type VARCHAR(10) NOT NULL COMMENT '收支方向 IN/OUT',
  amount DECIMAL(10,2) NOT NULL COMMENT '金额',
  balance_before DECIMAL(10,2) NOT NULL COMMENT '变动前余额',
  balance_after DECIMAL(10,2) NOT NULL COMMENT '变动后余额',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_transaction_record_txn_no (txn_no),
  KEY idx_biz_transaction_record_user_id_create_time (user_id, create_time),
  KEY idx_biz_transaction_record_order_id (order_id),
  KEY idx_biz_transaction_record_recharge_id (recharge_id),
  CONSTRAINT fk_biz_transaction_record_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_biz_transaction_record_order_id FOREIGN KEY (order_id) REFERENCES biz_reservation_order (id),
  CONSTRAINT fk_biz_transaction_record_recharge_id FOREIGN KEY (recharge_id) REFERENCES biz_recharge_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易流水表';

CREATE TABLE biz_settlement_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  settlement_no VARCHAR(50) NOT NULL COMMENT '结算单号',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  instrument_id BIGINT NOT NULL COMMENT '仪器ID',
  bill_type VARCHAR(20) NOT NULL COMMENT '账单类型 INTERNAL/EXTERNAL',
  price_desc VARCHAR(255) DEFAULT NULL COMMENT '价格说明',
  estimated_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预估金额',
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  final_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最终金额',
  settle_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '结算状态 PENDING/CONFIRMED/VOID/REFUNDED',
  settled_time DATETIME DEFAULT NULL COMMENT '结算时间',
  operator_user_id BIGINT DEFAULT NULL COMMENT '结算操作人ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_settlement_record_settlement_no (settlement_no),
  UNIQUE KEY uk_biz_settlement_record_order_id (order_id),
  KEY idx_biz_settlement_record_user_id (user_id),
  KEY idx_biz_settlement_record_instrument_id (instrument_id),
  CONSTRAINT fk_biz_settlement_record_order_id FOREIGN KEY (order_id) REFERENCES biz_reservation_order (id),
  CONSTRAINT fk_biz_settlement_record_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_biz_settlement_record_instrument_id FOREIGN KEY (instrument_id) REFERENCES biz_instrument (id),
  CONSTRAINT fk_biz_settlement_record_operator_user_id FOREIGN KEY (operator_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算记录表';

-- =========================
-- 5. 内容、消息、日志、统计表
-- =========================

CREATE TABLE content_notice (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  category VARCHAR(20) NOT NULL COMMENT '分类 NOTICE/INSTRUMENT_NOTICE',
  summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
  content TEXT NOT NULL COMMENT '内容',
  cover_url VARCHAR(255) DEFAULT NULL COMMENT '封面图',
  instrument_id BIGINT DEFAULT NULL COMMENT '关联仪器ID',
  top_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶',
  publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态 DRAFT/PUBLISHED/OFFLINE',
  publish_time DATETIME DEFAULT NULL COMMENT '发布时间',
  view_count INT NOT NULL DEFAULT 0 COMMENT '浏览量',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  KEY idx_content_notice_publish_status_publish_time (publish_status, publish_time),
  KEY idx_content_notice_instrument_id (instrument_id),
  CONSTRAINT fk_content_notice_instrument_id FOREIGN KEY (instrument_id) REFERENCES biz_instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

CREATE TABLE content_help_doc (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  doc_type VARCHAR(20) NOT NULL COMMENT '文档类型 HELP/FAQ/TEMPLATE/PROCESS',
  summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
  content TEXT DEFAULT NULL COMMENT '内容',
  file_url VARCHAR(255) DEFAULT NULL COMMENT '附件地址',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态 DRAFT/PUBLISHED/OFFLINE',
  publish_time DATETIME DEFAULT NULL COMMENT '发布时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  KEY idx_content_help_doc_publish_status_publish_time (publish_status, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帮助文档表';

CREATE TABLE sys_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '接收用户ID',
  msg_type VARCHAR(20) NOT NULL COMMENT '消息类型 ORDER/AUDIT/FINANCE/SYSTEM',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  content VARCHAR(500) NOT NULL COMMENT '内容',
  biz_type VARCHAR(20) DEFAULT NULL COMMENT '业务类型',
  biz_id BIGINT DEFAULT NULL COMMENT '业务ID',
  read_status TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态 0未读1已读',
  read_time DATETIME DEFAULT NULL COMMENT '阅读时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_sys_message_user_id_read_status_create_time (user_id, read_status, create_time),
  CONSTRAINT fk_sys_message_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息表';

CREATE TABLE sys_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
  module_name VARCHAR(50) NOT NULL COMMENT '模块名称',
  action_name VARCHAR(50) NOT NULL COMMENT '动作名称',
  request_method VARCHAR(10) NOT NULL COMMENT '请求方法',
  request_uri VARCHAR(255) NOT NULL COMMENT '请求URI',
  request_ip VARCHAR(50) DEFAULT NULL COMMENT '请求IP',
  result_code INT NOT NULL DEFAULT 0 COMMENT '结果编码',
  result_msg VARCHAR(255) DEFAULT NULL COMMENT '结果消息',
  biz_id BIGINT DEFAULT NULL COMMENT '关联业务ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_sys_operation_log_user_id_create_time (user_id, create_time),
  KEY idx_sys_operation_log_module_name_create_time (module_name, create_time),
  CONSTRAINT fk_sys_operation_log_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

CREATE TABLE stat_daily_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  stat_date DATE NOT NULL COMMENT '统计日期',
  instrument_total INT NOT NULL DEFAULT 0 COMMENT '仪器总数',
  instrument_open_total INT NOT NULL DEFAULT 0 COMMENT '开放仪器数',
  reservable_instrument_total INT NOT NULL DEFAULT 0 COMMENT '可预约仪器数',
  machine_order_total INT NOT NULL DEFAULT 0 COMMENT '上机订单数',
  sample_order_total INT NOT NULL DEFAULT 0 COMMENT '送样订单数',
  completed_order_total INT NOT NULL DEFAULT 0 COMMENT '完成订单数',
  effective_work_minutes INT NOT NULL DEFAULT 0 COMMENT '有效工作总分钟数',
  external_service_minutes INT NOT NULL DEFAULT 0 COMMENT '对外服务总分钟数',
  income_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '收入金额',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_stat_daily_snapshot_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日统计快照表';

-- =========================
-- 7. 预约冲突判断示例SQL
-- =========================
-- SELECT COUNT(1)
-- FROM biz_reservation_order
-- WHERE instrument_id = ?
--   AND order_type = 'MACHINE'
--   AND deleted = 0
--   AND order_status IN ('PENDING_AUDIT', 'WAITING_USE', 'IN_USE')
--   AND reserve_start < ?
--   AND reserve_end > ?;

SET FOREIGN_KEY_CHECKS = 1;


-- =========================================================
-- Appended upgrade: 2026-03-10
-- Source: upgrade_20260310_instrument_fields.sql
-- =========================================================

-- Incremental upgrade for instrument detail enhancement (2026-03-10)
-- Compatible with MySQL 5.7 / 8.0 (without ADD COLUMN IF NOT EXISTS).

USE instrument_share_platform;

SET @db := DATABASE();

DROP PROCEDURE IF EXISTS add_col_if_missing;
DELIMITER $$
CREATE PROCEDURE add_col_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_definition_sql VARCHAR(1024)
)
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(1)
      INTO v_count
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db
       AND TABLE_NAME = p_table_name
       AND COLUMN_NAME = p_column_name;

    IF v_count = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_definition_sql);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_col_if_missing('biz_instrument', 'asset_no',
    'asset_no VARCHAR(50) DEFAULT NULL COMMENT ''资产编号'' AFTER brand');
CALL add_col_if_missing('biz_instrument', 'manufacturer',
    'manufacturer VARCHAR(100) DEFAULT NULL COMMENT ''生产厂家'' AFTER asset_no');
CALL add_col_if_missing('biz_instrument', 'supplier',
    'supplier VARCHAR(100) DEFAULT NULL COMMENT ''供应商'' AFTER manufacturer');
CALL add_col_if_missing('biz_instrument', 'origin_country',
    'origin_country VARCHAR(50) DEFAULT NULL COMMENT ''制造国家'' AFTER supplier');
CALL add_col_if_missing('biz_instrument', 'purchase_date',
    'purchase_date DATE DEFAULT NULL COMMENT ''购置日期'' AFTER origin_country');
CALL add_col_if_missing('biz_instrument', 'production_date',
    'production_date DATE DEFAULT NULL COMMENT ''生产日期'' AFTER purchase_date');
CALL add_col_if_missing('biz_instrument', 'equipment_source',
    'equipment_source VARCHAR(50) DEFAULT NULL COMMENT ''设备来源'' AFTER production_date');
CALL add_col_if_missing('biz_instrument', 'service_contact_name',
    'service_contact_name VARCHAR(50) DEFAULT NULL COMMENT ''使用联系人'' AFTER equipment_source');
CALL add_col_if_missing('biz_instrument', 'service_contact_phone',
    'service_contact_phone VARCHAR(20) DEFAULT NULL COMMENT ''联系人电话'' AFTER service_contact_name');
CALL add_col_if_missing('biz_instrument', 'technical_specs',
    'technical_specs TEXT DEFAULT NULL COMMENT ''主要技术指标'' AFTER notice_text');
CALL add_col_if_missing('biz_instrument', 'main_functions',
    'main_functions TEXT DEFAULT NULL COMMENT ''主要功能'' AFTER technical_specs');
CALL add_col_if_missing('biz_instrument', 'service_content',
    'service_content TEXT DEFAULT NULL COMMENT ''服务内容'' AFTER main_functions');
CALL add_col_if_missing('biz_instrument', 'user_notice',
    'user_notice TEXT DEFAULT NULL COMMENT ''用户须知'' AFTER service_content');
CALL add_col_if_missing('biz_instrument', 'charge_standard',
    'charge_standard TEXT DEFAULT NULL COMMENT ''收费标准说明'' AFTER user_notice');

DROP PROCEDURE IF EXISTS add_col_if_missing;



-- =========================================================
-- Appended upgrade: 2026-03-13
-- Source: upgrade_20260313_open_rule_multi_days.sql
-- =========================================================

-- =========================================================
-- 开放规则多星期改造（严格迁移脚本）
-- 目标库：instrument_share_platform
-- 执行日期：2026-03-13
-- 说明：
-- 1) 新增 week_days 字段（逗号分隔：1,2,3...7）
-- 2) 历史数据回填：week_days = week_day
-- 3) 保留 week_day 作为兼容字段（取 week_days 的首个星期）
-- =========================================================

SET NAMES utf8mb4;
USE instrument_share_platform;

SET sql_safe_updates = 0;

-- 1) 新增字段（若不存在）
SET @has_week_days := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'biz_instrument_open_rule'
    AND COLUMN_NAME = 'week_days'
);

SET @ddl_add_week_days := IF(
  @has_week_days = 0,
  'ALTER TABLE biz_instrument_open_rule ADD COLUMN week_days VARCHAR(32) NOT NULL DEFAULT '''' COMMENT ''开放星期，逗号分隔：1-7'' AFTER week_day',
  'SELECT ''skip add week_days'' as msg'
);
PREPARE stmt_add_week_days FROM @ddl_add_week_days;
EXECUTE stmt_add_week_days;
DEALLOCATE PREPARE stmt_add_week_days;

-- 2) 回填 week_days（仅回填空值，避免覆盖已有数据）
UPDATE biz_instrument_open_rule
SET week_days = CAST(week_day AS CHAR)
WHERE deleted = 0
  AND week_day IS NOT NULL
  AND (week_days IS NULL OR week_days = '');

-- 3) 兜底清理：任何残留空值统一设为周一（防止后续逻辑空值）
UPDATE biz_instrument_open_rule
SET week_days = '1'
WHERE week_days IS NULL OR week_days = '';

-- 4) 规范化 week_day：同步为 week_days 的首个值（兼容旧逻辑）
UPDATE biz_instrument_open_rule
SET week_day = CAST(SUBSTRING_INDEX(week_days, ',', 1) AS UNSIGNED)
WHERE week_days IS NOT NULL
  AND week_days <> '';

-- 5) 建立索引（若不存在）
SET @has_idx_week_day := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'biz_instrument_open_rule'
    AND INDEX_NAME = 'idx_open_rule_week_day'
);

SET @ddl_add_idx_week_day := IF(
  @has_idx_week_day = 0,
  'CREATE INDEX idx_open_rule_week_day ON biz_instrument_open_rule(week_day)',
  'SELECT ''skip create idx_open_rule_week_day'' as msg'
);
PREPARE stmt_add_idx_week_day FROM @ddl_add_idx_week_day;
EXECUTE stmt_add_idx_week_day;
DEALLOCATE PREPARE stmt_add_idx_week_day;

-- 6) 校验结果
SELECT
  COUNT(1) AS total_rules,
  SUM(CASE WHEN week_days IS NULL OR week_days = '' THEN 1 ELSE 0 END) AS invalid_week_days
FROM biz_instrument_open_rule;

-- =========================================================
-- Appended upgrade pack: 2026-03-16 ~ 2026-03-19
-- 说明：将后续增量能力并入总建表脚本，确保全新初始化即为最新结构。
-- =========================================================

-- 1) 仪器培训资格表
CREATE TABLE IF NOT EXISTS biz_instrument_training_qualification (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  instrument_id BIGINT NOT NULL COMMENT '仪器ID',
  status VARCHAR(20) NOT NULL DEFAULT 'PASSED' COMMENT '资格状态 PASSED/REVOKED/EXPIRED',
  pass_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通过时间',
  expire_time DATETIME DEFAULT NULL COMMENT '失效时间（为空表示长期有效）',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_training_user_instrument (user_id, instrument_id),
  KEY idx_training_instrument_status (instrument_id, status),
  KEY idx_training_user_status (user_id, status),
  CONSTRAINT fk_training_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_training_instrument FOREIGN KEY (instrument_id) REFERENCES biz_instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仪器培训资格表';

-- 2) 充值审核双人复核字段
SET @has_review_status := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'biz_recharge_order'
    AND COLUMN_NAME = 'review_status'
);
SET @ddl_recharge_review := IF(
  @has_review_status = 0,
  'ALTER TABLE biz_recharge_order
     ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT ''NONE'' COMMENT ''复核状态：NONE/PENDING/PASS'' AFTER status,
     ADD COLUMN first_audit_user_id BIGINT NULL COMMENT ''初审人ID'' AFTER audit_time,
     ADD COLUMN first_audit_time DATETIME NULL COMMENT ''初审时间'' AFTER first_audit_user_id,
     ADD COLUMN second_audit_user_id BIGINT NULL COMMENT ''复核人ID'' AFTER first_audit_time,
     ADD COLUMN second_audit_time DATETIME NULL COMMENT ''复核时间'' AFTER second_audit_user_id',
  'SELECT ''skip alter biz_recharge_order for review'' AS msg'
);
PREPARE stmt_recharge_review FROM @ddl_recharge_review;
EXECUTE stmt_recharge_review;
DEALLOCATE PREPARE stmt_recharge_review;

UPDATE biz_recharge_order
SET review_status = 'NONE'
WHERE review_status IS NULL OR review_status = '';

-- 3) 财务异常处理表
CREATE TABLE IF NOT EXISTS biz_finance_anomaly_handle (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  anomaly_type VARCHAR(50) NOT NULL COMMENT '异常类型',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  settlement_id BIGINT NULL COMMENT '结算单ID',
  handle_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/RESOLVED',
  handle_comment VARCHAR(500) NULL COMMENT '处理备注',
  handler_user_id BIGINT NULL COMMENT '处理人',
  handle_time DATETIME NULL COMMENT '处理时间',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  update_time DATETIME NOT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_anomaly_type_order (anomaly_type, order_id),
  KEY idx_anomaly_handle_status (handle_status),
  KEY idx_anomaly_handle_time (handle_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务异常处理记录';

-- 4) 用户审核日志表
CREATE TABLE IF NOT EXISTS sys_user_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '目标用户ID',
  action_type VARCHAR(30) NOT NULL COMMENT '动作类型：REGISTER_AUDIT/STATUS_CHANGE/CREATE/UPDATE',
  action_result VARCHAR(30) NOT NULL COMMENT '动作结果：PASS/ENABLED/DISABLED/REJECTED等',
  operator_user_id BIGINT NULL COMMENT '操作人ID',
  operator_role VARCHAR(50) NULL COMMENT '操作人角色',
  remark VARCHAR(500) NULL COMMENT '备注',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user_audit_user_id (user_id),
  KEY idx_user_audit_create_time (create_time),
  KEY idx_user_audit_action (action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户审核与状态变更记录';

-- 5) 经费预算表
CREATE TABLE IF NOT EXISTS biz_finance_budget (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  budget_no VARCHAR(50) NOT NULL COMMENT '预算单号',
  budget_year INT NOT NULL COMMENT '预算年度',
  department_id BIGINT DEFAULT NULL COMMENT '部门ID（为空表示全平台）',
  instrument_id BIGINT DEFAULT NULL COMMENT '仪器ID（为空表示部门级预算）',
  budget_amount DECIMAL(12,2) NOT NULL COMMENT '预算金额',
  warning_ratio DECIMAL(5,2) NOT NULL DEFAULT 80.00 COMMENT '预警阈值百分比',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  operator_user_id BIGINT NOT NULL COMMENT '操作人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_finance_budget_no (budget_no),
  UNIQUE KEY uk_biz_finance_budget_scope (budget_year, department_id, instrument_id),
  KEY idx_biz_finance_budget_department_id (department_id),
  KEY idx_biz_finance_budget_instrument_id (instrument_id),
  CONSTRAINT fk_biz_finance_budget_department_id FOREIGN KEY (department_id) REFERENCES sys_department (id),
  CONSTRAINT fk_biz_finance_budget_instrument_id FOREIGN KEY (instrument_id) REFERENCES biz_instrument (id),
  CONSTRAINT fk_biz_finance_budget_operator_user_id FOREIGN KEY (operator_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='经费预算表';

-- 6) 经费支出表
CREATE TABLE IF NOT EXISTS biz_finance_expense (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  expense_no VARCHAR(50) NOT NULL COMMENT '支出单号',
  instrument_id BIGINT NOT NULL COMMENT '仪器ID',
  department_id BIGINT DEFAULT NULL COMMENT '归属部门ID',
  expense_type VARCHAR(30) NOT NULL COMMENT '支出类型 MAINTENANCE/REPAIR/CALIBRATION/OTHER',
  amount DECIMAL(10,2) NOT NULL COMMENT '支出金额',
  title VARCHAR(200) NOT NULL COMMENT '支出标题',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  expense_time DATETIME NOT NULL COMMENT '支出时间',
  operator_user_id BIGINT NOT NULL COMMENT '登记人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_finance_expense_expense_no (expense_no),
  KEY idx_biz_finance_expense_instrument_id (instrument_id),
  KEY idx_biz_finance_expense_department_id (department_id),
  KEY idx_biz_finance_expense_expense_time (expense_time),
  CONSTRAINT fk_biz_finance_expense_instrument_id FOREIGN KEY (instrument_id) REFERENCES biz_instrument (id),
  CONSTRAINT fk_biz_finance_expense_department_id FOREIGN KEY (department_id) REFERENCES sys_department (id),
  CONSTRAINT fk_biz_finance_expense_operator_user_id FOREIGN KEY (operator_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='经费支出记录表';

-- 7) 复核相关索引
SET @has_idx_recharge_status_create_time := (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_recharge_order' AND INDEX_NAME = 'idx_recharge_status_create_time'
);
SET @ddl_idx_recharge_status_create_time := IF(
  @has_idx_recharge_status_create_time = 0,
  'CREATE INDEX idx_recharge_status_create_time ON biz_recharge_order(status, create_time)',
  'SELECT ''skip idx_recharge_status_create_time'' AS msg'
);
PREPARE stmt_idx_recharge_status_create_time FROM @ddl_idx_recharge_status_create_time;
EXECUTE stmt_idx_recharge_status_create_time;
DEALLOCATE PREPARE stmt_idx_recharge_status_create_time;

SET @has_idx_recharge_review_status := (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_recharge_order' AND INDEX_NAME = 'idx_recharge_review_status'
);
SET @ddl_idx_recharge_review_status := IF(
  @has_idx_recharge_review_status = 0,
  'CREATE INDEX idx_recharge_review_status ON biz_recharge_order(review_status)',
  'SELECT ''skip idx_recharge_review_status'' AS msg'
);
PREPARE stmt_idx_recharge_review_status FROM @ddl_idx_recharge_review_status;
EXECUTE stmt_idx_recharge_review_status;
DEALLOCATE PREPARE stmt_idx_recharge_review_status;
