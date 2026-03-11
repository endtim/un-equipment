-- Idempotent seed data for schema: docs/sql/un-equipment.sql
-- Target DB: instrument_share_platform

SET NAMES utf8mb4;
USE instrument_share_platform;

SET sql_safe_updates = 0;
-- Avoid long transaction lock when app is running.
-- If you need all-or-nothing execution, manually wrap sections with START TRANSACTION / COMMIT.

-- ---------------------------------------------------------
-- 1) Departments / Roles / Users
-- ---------------------------------------------------------
INSERT INTO sys_department
(parent_id, dept_name, dept_code, leader_user_id, phone, email, sort_no, status, remark, create_by, create_time, update_by, update_time, deleted)
VALUES
(NULL, '分析测试中心', 'TEST_CENTER', NULL, '0791-88880001', 'test_center@demo.edu.cn', 1, 'ENABLED', 'SEED', NULL, NOW(), NULL, NOW(), 0),
(NULL, '化学化工学院', 'CHEM_COLLEGE', NULL, '0791-88880002', 'chem@demo.edu.cn', 2, 'ENABLED', 'SEED', NULL, NOW(), NULL, NOW(), 0),
(NULL, '材料科学学院', 'MAT_COLLEGE', NULL, '0791-88880003', 'mat@demo.edu.cn', 3, 'ENABLED', 'SEED', NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
dept_name = VALUES(dept_name),
phone = VALUES(phone),
email = VALUES(email),
sort_no = VALUES(sort_no),
status = VALUES(status),
remark = VALUES(remark),
deleted = 0,
update_time = NOW();

INSERT INTO sys_role
(role_name, role_code, status, remark, create_time, update_time, deleted)
VALUES
('平台管理员', 'ADMIN', 'ENABLED', 'SEED', NOW(), NOW(), 0),
('仪器负责人', 'INSTRUMENT_OWNER', 'ENABLED', 'SEED', NOW(), NOW(), 0),
('部门管理员', 'DEPT_MANAGER', 'ENABLED', 'SEED', NOW(), NOW(), 0),
('校内用户', 'INTERNAL_USER', 'ENABLED', 'SEED', NOW(), NOW(), 0),
('校外用户', 'EXTERNAL_USER', 'ENABLED', 'SEED', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
role_name = VALUES(role_name),
status = VALUES(status),
remark = VALUES(remark),
deleted = 0,
update_time = NOW();

SELECT id INTO @dept_test FROM sys_department WHERE dept_code = 'TEST_CENTER' LIMIT 1;
SELECT id INTO @dept_chem FROM sys_department WHERE dept_code = 'CHEM_COLLEGE' LIMIT 1;
SELECT id INTO @dept_mat  FROM sys_department WHERE dept_code = 'MAT_COLLEGE' LIMIT 1;

INSERT INTO sys_user
(username, password, real_name, user_type, user_no, gender, phone, email, avatar_url, auth_type, department_id, unit_name, title_name, status, last_login_time, remark, create_time, update_time, deleted)
VALUES
('admin', '$2b$10$QuyNHFfR/NYgQEwFqdHblezl5iFBBytD2O/0vkoZZbiLqxdAMEBiC', '系统管理员', 'INTERNAL', 'U0001', 'M', '13900000001', 'admin@demo.edu.cn', NULL, 'LOCAL', @dept_test, '分析测试中心', '管理员', 'ENABLED', NULL, 'SEED', NOW(), NOW(), 0),
('owner_zhang', '$2b$10$QuyNHFfR/NYgQEwFqdHblezl5iFBBytD2O/0vkoZZbiLqxdAMEBiC', '张老师', 'INTERNAL', 'U0002', 'M', '13900000002', 'owner_zhang@demo.edu.cn', NULL, 'LOCAL', @dept_chem, '化学化工学院', '仪器平台主管', 'ENABLED', NULL, 'SEED', NOW(), NOW(), 0),
('teacher_wang', '$2b$10$QuyNHFfR/NYgQEwFqdHblezl5iFBBytD2O/0vkoZZbiLqxdAMEBiC', '王老师', 'INTERNAL', 'U0003', 'F', '13900000003', 'teacher_wang@demo.edu.cn', NULL, 'LOCAL', @dept_mat, '材料科学学院', '讲师', 'ENABLED', NULL, 'SEED', NOW(), NOW(), 0),
('external_li', '$2b$10$QuyNHFfR/NYgQEwFqdHblezl5iFBBytD2O/0vkoZZbiLqxdAMEBiC', '李工', 'EXTERNAL', 'U0004', 'M', '13900000004', 'external_li@demo.com', NULL, 'LOCAL', @dept_test, '校外合作单位', '工程师', 'ENABLED', NULL, 'SEED', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
password = VALUES(password),
real_name = VALUES(real_name),
user_type = VALUES(user_type),
user_no = VALUES(user_no),
gender = VALUES(gender),
phone = VALUES(phone),
email = VALUES(email),
department_id = VALUES(department_id),
unit_name = VALUES(unit_name),
title_name = VALUES(title_name),
status = VALUES(status),
remark = VALUES(remark),
deleted = 0,
update_time = NOW();

SELECT id INTO @u_admin FROM sys_user WHERE username = 'admin' LIMIT 1;
SELECT id INTO @u_owner FROM sys_user WHERE username = 'owner_zhang' LIMIT 1;
SELECT id INTO @u_teacher FROM sys_user WHERE username = 'teacher_wang' LIMIT 1;
SELECT id INTO @u_external FROM sys_user WHERE username = 'external_li' LIMIT 1;

SELECT id INTO @r_admin FROM sys_role WHERE role_code = 'ADMIN' LIMIT 1;
SELECT id INTO @r_owner FROM sys_role WHERE role_code = 'INSTRUMENT_OWNER' LIMIT 1;
SELECT id INTO @r_internal FROM sys_role WHERE role_code = 'INTERNAL_USER' LIMIT 1;
SELECT id INTO @r_external FROM sys_role WHERE role_code = 'EXTERNAL_USER' LIMIT 1;

DELETE FROM sys_user_role WHERE user_id IN (@u_admin, @u_owner, @u_teacher, @u_external);
INSERT INTO sys_user_role (user_id, role_id, create_time)
VALUES
(@u_admin, @r_admin, NOW()),
(@u_owner, @r_owner, NOW()),
(@u_teacher, @r_internal, NOW()),
(@u_external, @r_external, NOW());

INSERT INTO biz_account
(user_id, balance, frozen_amount, total_recharge, total_consume, status, create_time, update_time)
VALUES
(@u_admin, 10000.00, 0.00, 10000.00, 0.00, 'ENABLED', NOW(), NOW()),
(@u_owner, 3000.00, 0.00, 3000.00, 0.00, 'ENABLED', NOW(), NOW()),
(@u_teacher, 2500.00, 0.00, 2500.00, 0.00, 'ENABLED', NOW(), NOW()),
(@u_external, 1200.00, 0.00, 1200.00, 0.00, 'ENABLED', NOW(), NOW())
ON DUPLICATE KEY UPDATE
balance = VALUES(balance),
frozen_amount = VALUES(frozen_amount),
total_recharge = VALUES(total_recharge),
total_consume = VALUES(total_consume),
status = VALUES(status),
update_time = NOW();

-- ---------------------------------------------------------
-- 2) Instrument master data
-- ---------------------------------------------------------
INSERT INTO biz_instrument_category
(parent_id, category_name, category_code, sort_no, status, create_time, update_time, deleted)
VALUES
(NULL, '光谱分析', 'SPECTRUM', 1, 'ENABLED', NOW(), NOW(), 0),
(NULL, '显微成像', 'MICROSCOPY', 2, 'ENABLED', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
category_name = VALUES(category_name),
sort_no = VALUES(sort_no),
status = VALUES(status),
deleted = 0,
update_time = NOW();

SELECT id INTO @cat_spectrum FROM biz_instrument_category WHERE category_code = 'SPECTRUM' LIMIT 1;
SELECT id INTO @cat_micro FROM biz_instrument_category WHERE category_code = 'MICROSCOPY' LIMIT 1;

INSERT INTO biz_instrument
(instrument_no, instrument_name, model, brand, asset_no, manufacturer, supplier, origin_country, purchase_date, production_date, equipment_source, service_contact_name, service_contact_phone, category_id, department_id, owner_user_id, location, status, open_mode, open_status, support_external, need_audit, require_training, booking_unit, price_internal, price_external, min_reserve_minutes, max_reserve_minutes, step_minutes, cover_url, intro, usage_desc, sample_desc, notice_text, technical_specs, main_functions, service_content, user_notice, charge_standard, is_hot, sort_no, create_time, update_time, deleted)
VALUES
('INS-1001', '高效液相色谱仪', 'LC-20A', 'Shimadzu', 'ASSET-1001', '岛津', '江西仪器供应中心', '日本', '2019-06-01', '2019-03-15', '购置', '张老师', '13900000002', @cat_spectrum, @dept_chem, @u_owner, '化工楼A201', 'NORMAL', 'BOTH', 1, 1, 1, 0, 'HOUR', 120.00, 200.00, 60, 480, 30, 'https://example.com/cover/hplc.jpg', 'SEED-HPLC', '预约前请完成培训登记', '支持送样测试，结果48小时内回传', '请提前24小时预约', '适用于有机物分离分析，检测灵敏度高，稳定性好。', '用于样品成分分离、定量分析和方法开发。', '提供上机测试与送样检测两种服务。', '请按预约时段到场，规范使用并完成登记。', '校内120元/小时，校外200元/小时，送样按项目协商。', 1, 1, NOW(), NOW(), 0),
('INS-1002', '扫描电子显微镜', 'SU5000', 'Hitachi', 'ASSET-1002', '日立', '江西精测设备有限公司', '日本', '2020-09-10', '2020-07-20', '购置', '李工程师', '13900000011', @cat_micro, @dept_mat, @u_owner, '材料楼B305', 'NORMAL', 'BOTH', 1, 1, 1, 1, 'HOUR', 180.00, 260.00, 60, 360, 30, 'https://example.com/cover/sem.jpg', 'SEED-SEM', '使用前需通过上机培训', '支持危险样品申报', '请按规范提交样品信息', '分辨率高，支持多倍率成像和元素分析扩展。', '用于材料形貌观察、缺陷分析和微结构表征。', '提供预约上机、送样检测及结果解读。', '危险样品需提前申报并按安全规范提交。', '校内180元/小时，校外260元/小时。', 1, 2, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
instrument_name = VALUES(instrument_name),
model = VALUES(model),
brand = VALUES(brand),
asset_no = VALUES(asset_no),
manufacturer = VALUES(manufacturer),
supplier = VALUES(supplier),
origin_country = VALUES(origin_country),
purchase_date = VALUES(purchase_date),
production_date = VALUES(production_date),
equipment_source = VALUES(equipment_source),
service_contact_name = VALUES(service_contact_name),
service_contact_phone = VALUES(service_contact_phone),
category_id = VALUES(category_id),
department_id = VALUES(department_id),
owner_user_id = VALUES(owner_user_id),
location = VALUES(location),
status = VALUES(status),
open_mode = VALUES(open_mode),
open_status = VALUES(open_status),
support_external = VALUES(support_external),
need_audit = VALUES(need_audit),
require_training = VALUES(require_training),
booking_unit = VALUES(booking_unit),
price_internal = VALUES(price_internal),
price_external = VALUES(price_external),
min_reserve_minutes = VALUES(min_reserve_minutes),
max_reserve_minutes = VALUES(max_reserve_minutes),
step_minutes = VALUES(step_minutes),
cover_url = VALUES(cover_url),
intro = VALUES(intro),
usage_desc = VALUES(usage_desc),
sample_desc = VALUES(sample_desc),
notice_text = VALUES(notice_text),
technical_specs = VALUES(technical_specs),
main_functions = VALUES(main_functions),
service_content = VALUES(service_content),
user_notice = VALUES(user_notice),
charge_standard = VALUES(charge_standard),
is_hot = VALUES(is_hot),
sort_no = VALUES(sort_no),
deleted = 0,
update_time = NOW();

SELECT id INTO @ins1 FROM biz_instrument WHERE instrument_no = 'INS-1001' LIMIT 1;
SELECT id INTO @ins2 FROM biz_instrument WHERE instrument_no = 'INS-1002' LIMIT 1;

DELETE FROM biz_instrument_open_rule WHERE instrument_id IN (@ins1, @ins2);
INSERT INTO biz_instrument_open_rule
(instrument_id, week_day, start_time, end_time, max_reserve_minutes, step_minutes, effective_start_date, effective_end_date, status, create_time, update_time, deleted)
VALUES
(@ins1, 1, '08:30:00', '17:30:00', 480, 30, CURDATE(), NULL, 'ENABLED', NOW(), NOW(), 0),
(@ins2, 2, '08:30:00', '17:30:00', 360, 30, CURDATE(), NULL, 'ENABLED', NOW(), NOW(), 0);

DELETE FROM biz_instrument_attachment WHERE instrument_id IN (@ins1, @ins2);
INSERT INTO biz_instrument_attachment
(instrument_id, file_name, file_url, file_type, sort_no, create_time, deleted)
VALUES
(@ins1, '高效液相色谱仪操作规程.pdf', 'https://example.com/files/INS-1001-SOP.pdf', 'MANUAL', 1, NOW(), 0),
(@ins2, '扫描电镜送样规范.docx', 'https://example.com/files/INS-1002-SAMPLE.docx', 'GUIDE', 1, NOW(), 0);

-- ---------------------------------------------------------
-- 3) Content / message
-- ---------------------------------------------------------
DELETE FROM content_notice WHERE title = 'SEED-平台预约流程通知';
INSERT INTO content_notice
(title, category, summary, content, cover_url, instrument_id, top_flag, publish_status, publish_time, view_count, create_time, update_time, deleted)
VALUES
('SEED-平台预约流程通知', 'NOTICE', '预约流程统一线上办理', '所有预约、送样、充值申请统一通过系统提交。', NULL, NULL, 1, 'PUBLISHED', NOW(), 0, NOW(), NOW(), 0);

DELETE FROM content_help_doc WHERE title = 'SEED-上机预约操作指南';
INSERT INTO content_help_doc
(title, doc_type, summary, content, file_url, sort_no, publish_status, publish_time, create_time, update_time, deleted)
VALUES
('SEED-上机预约操作指南', 'GUIDE', '演示账号完整流程', '1) 选择仪器 2) 提交预约 3) 等待审核 4) 签到使用 5) 完成结算', 'https://example.com/help/machine-guide.pdf', 1, 'PUBLISHED', NOW(), NOW(), NOW(), 0);

DELETE FROM sys_message WHERE user_id = @u_teacher AND title = 'SEED-欢迎使用平台';
INSERT INTO sys_message
(user_id, msg_type, title, content, biz_type, biz_id, read_status, read_time, create_time)
VALUES
(@u_teacher, 'SYSTEM', 'SEED-欢迎使用平台', '请前往预约服务浏览可用仪器。', 'WELCOME', NULL, 0, NULL, NOW());

-- ---------------------------------------------------------
-- 4) Finance seed
-- ---------------------------------------------------------
INSERT INTO biz_recharge_order
(recharge_no, user_id, amount, pay_method, voucher_url, status, remark, audit_user_id, audit_time, create_time, update_time)
VALUES
('RCG-SEED-001', @u_teacher, 300.00, 'OFFLINE', 'https://example.com/voucher/rcg-seed-001.jpg', 'PENDING', 'SEED-待审核', NULL, NULL, NOW(), NOW()),
('RCG-SEED-002', @u_teacher, 500.00, 'OFFLINE', 'https://example.com/voucher/rcg-seed-002.jpg', 'PASS', 'SEED-已通过', @u_admin, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY)
ON DUPLICATE KEY UPDATE
user_id = VALUES(user_id),
amount = VALUES(amount),
pay_method = VALUES(pay_method),
voucher_url = VALUES(voucher_url),
status = VALUES(status),
remark = VALUES(remark),
audit_user_id = VALUES(audit_user_id),
audit_time = VALUES(audit_time),
update_time = NOW();

SELECT id INTO @rcg2 FROM biz_recharge_order WHERE recharge_no = 'RCG-SEED-002' LIMIT 1;

INSERT INTO biz_transaction_record
(txn_no, user_id, order_id, recharge_id, txn_type, inout_type, amount, balance_before, balance_after, remark, create_time)
VALUES
('TXN-SEED-RECHARGE-002', @u_teacher, NULL, @rcg2, 'RECHARGE', 'IN', 500.00, 2000.00, 2500.00, 'SEED-历史充值到账', NOW() - INTERVAL 2 DAY)
ON DUPLICATE KEY UPDATE
recharge_id = VALUES(recharge_id),
remark = VALUES(remark),
create_time = VALUES(create_time);

-- ---------------------------------------------------------
-- 5) Orders and flow seed
-- ---------------------------------------------------------
INSERT INTO biz_reservation_order
(order_no, order_type, user_id, instrument_id, department_id, owner_user_id, contact_name, contact_phone, purpose, project_name, reserve_start, reserve_end, reserve_minutes, order_status, audit_status, pay_status, settlement_status, estimated_amount, final_amount, source, submit_time, approve_time, finish_time, cancel_reason, remark, create_time, update_time, deleted)
VALUES
('ORDM-SEED-001', 'MACHINE', @u_teacher, @ins1, @dept_mat, @u_owner, '王老师', '13900000003', '课程实验', '流程测试项目A', NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 1 DAY + INTERVAL 2 HOUR, 120, 'PENDING_AUDIT', 'PENDING', 'UNPAID', 'PENDING', 240.00, 240.00, 'WEB', NOW(), NULL, NULL, NULL, 'SEED-待审核上机订单', NOW(), NOW(), 0),
('ORDM-SEED-002', 'MACHINE', @u_teacher, @ins1, @dept_mat, @u_owner, '王老师', '13900000003', '科研测试', '流程测试项目B', NOW() + INTERVAL 2 DAY, NOW() + INTERVAL 2 DAY + INTERVAL 3 HOUR, 180, 'WAITING_USE', 'PASS', 'UNPAID', 'PENDING', 360.00, 360.00, 'WEB', NOW(), NOW(), NULL, NULL, 'SEED-可签到上机订单', NOW(), NOW(), 0),
('ORDS-SEED-001', 'SAMPLE', @u_teacher, @ins2, @dept_mat, @u_owner, '王老师', '13900000003', '送样分析', '流程测试项目C', NULL, NULL, 0, 'WAITING_RECEIVE', 'PASS', 'UNPAID', 'PENDING', 520.00, 520.00, 'WEB', NOW(), NOW(), NULL, NULL, 'SEED-可接样订单', NOW(), NOW(), 0),
('ORDM-SEED-003', 'MACHINE', @u_teacher, @ins1, @dept_mat, @u_owner, '王老师', '13900000003', '历史数据', '流程测试项目D', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, 120, 'COMPLETED', 'PASS', 'PAID', 'CONFIRMED', 240.00, 240.00, 'WEB', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY, NULL, 'SEED-历史完成订单', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 3 DAY, 0)
ON DUPLICATE KEY UPDATE
order_type = VALUES(order_type),
user_id = VALUES(user_id),
instrument_id = VALUES(instrument_id),
department_id = VALUES(department_id),
owner_user_id = VALUES(owner_user_id),
contact_name = VALUES(contact_name),
contact_phone = VALUES(contact_phone),
purpose = VALUES(purpose),
project_name = VALUES(project_name),
reserve_start = VALUES(reserve_start),
reserve_end = VALUES(reserve_end),
reserve_minutes = VALUES(reserve_minutes),
order_status = VALUES(order_status),
audit_status = VALUES(audit_status),
pay_status = VALUES(pay_status),
settlement_status = VALUES(settlement_status),
estimated_amount = VALUES(estimated_amount),
final_amount = VALUES(final_amount),
source = VALUES(source),
submit_time = VALUES(submit_time),
approve_time = VALUES(approve_time),
finish_time = VALUES(finish_time),
cancel_reason = VALUES(cancel_reason),
remark = VALUES(remark),
deleted = 0,
update_time = NOW();

SELECT id INTO @ord_m1 FROM biz_reservation_order WHERE order_no = 'ORDM-SEED-001' LIMIT 1;
SELECT id INTO @ord_m2 FROM biz_reservation_order WHERE order_no = 'ORDM-SEED-002' LIMIT 1;
SELECT id INTO @ord_s1 FROM biz_reservation_order WHERE order_no = 'ORDS-SEED-001' LIMIT 1;
SELECT id INTO @ord_m3 FROM biz_reservation_order WHERE order_no = 'ORDM-SEED-003' LIMIT 1;

INSERT INTO biz_sample_order
(order_id, sample_name, sample_count, sample_type, sample_spec, test_requirement, danger_flag, danger_desc, receive_status, received_time, receiver_user_id, testing_status, result_summary, create_time, update_time)
VALUES
(@ord_s1, '催化剂样品A', 2, 'POWDER', '50g/袋', '元素组成分析', 0, NULL, 'WAITING_RECEIVE', NULL, NULL, 'WAITING_RECEIVE', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
sample_name = VALUES(sample_name),
sample_count = VALUES(sample_count),
sample_type = VALUES(sample_type),
sample_spec = VALUES(sample_spec),
test_requirement = VALUES(test_requirement),
danger_flag = VALUES(danger_flag),
danger_desc = VALUES(danger_desc),
receive_status = VALUES(receive_status),
received_time = VALUES(received_time),
receiver_user_id = VALUES(receiver_user_id),
testing_status = VALUES(testing_status),
result_summary = VALUES(result_summary),
update_time = NOW();

INSERT INTO biz_usage_record
(order_id, instrument_id, operator_user_id, checkin_time, start_time, end_time, actual_minutes, abnormal_flag, abnormal_desc, owner_confirm_user_id, owner_confirm_time, create_time, update_time)
VALUES
(@ord_m3, @ins1, @u_teacher, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, 120, 0, NULL, @u_owner, NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY)
ON DUPLICATE KEY UPDATE
instrument_id = VALUES(instrument_id),
operator_user_id = VALUES(operator_user_id),
checkin_time = VALUES(checkin_time),
start_time = VALUES(start_time),
end_time = VALUES(end_time),
actual_minutes = VALUES(actual_minutes),
abnormal_flag = VALUES(abnormal_flag),
abnormal_desc = VALUES(abnormal_desc),
owner_confirm_user_id = VALUES(owner_confirm_user_id),
owner_confirm_time = VALUES(owner_confirm_time),
update_time = NOW();

DELETE FROM biz_reservation_audit WHERE order_id IN (@ord_m1, @ord_m2, @ord_s1, @ord_m3);
INSERT INTO biz_reservation_audit
(order_id, node_no, auditor_id, auditor_role, audit_result, audit_opinion, audit_time, create_time)
VALUES
(@ord_m1, 1, @u_teacher, 'INTERNAL_USER', 'PENDING', 'SUBMIT: Machine order submitted', NOW(), NOW()),
(@ord_m2, 1, @u_teacher, 'INTERNAL_USER', 'PENDING', 'SUBMIT: Machine order submitted', NOW(), NOW()),
(@ord_m2, 2, @u_owner, 'INSTRUMENT_OWNER', 'PASS', 'APPROVE: Approved', NOW(), NOW()),
(@ord_s1, 1, @u_teacher, 'INTERNAL_USER', 'PENDING', 'SUBMIT: Sample order submitted', NOW(), NOW()),
(@ord_s1, 2, @u_owner, 'INSTRUMENT_OWNER', 'PASS', 'APPROVE: Approved', NOW(), NOW()),
(@ord_m3, 1, @u_teacher, 'INTERNAL_USER', 'PENDING', 'SUBMIT: Machine order submitted', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY),
(@ord_m3, 2, @u_owner, 'INSTRUMENT_OWNER', 'PASS', 'APPROVE: Approved', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
(@ord_m3, 3, @u_owner, 'INSTRUMENT_OWNER', 'PASS', 'SETTLE: Order settled', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY);

INSERT INTO biz_settlement_record
(settlement_no, order_id, user_id, instrument_id, bill_type, price_desc, estimated_amount, discount_amount, final_amount, settle_status, settled_time, operator_user_id, create_time)
VALUES
('SET-SEED-003', @ord_m3, @u_teacher, @ins1, 'INTERNAL', 'Auto generated from reservation order', 240.00, 0.00, 240.00, 'CONFIRMED', NOW() - INTERVAL 3 DAY, @u_owner, NOW() - INTERVAL 3 DAY)
ON DUPLICATE KEY UPDATE
order_id = VALUES(order_id),
user_id = VALUES(user_id),
instrument_id = VALUES(instrument_id),
bill_type = VALUES(bill_type),
final_amount = VALUES(final_amount),
settle_status = VALUES(settle_status),
settled_time = VALUES(settled_time),
operator_user_id = VALUES(operator_user_id),
create_time = VALUES(create_time);

INSERT INTO biz_transaction_record
(txn_no, user_id, order_id, recharge_id, txn_type, inout_type, amount, balance_before, balance_after, remark, create_time)
VALUES
('TXN-SEED-CONSUME-003', @u_teacher, @ord_m3, NULL, 'CONSUME', 'OUT', -240.00, 2740.00, 2500.00, 'SEED-Order settlement', NOW() - INTERVAL 3 DAY)
ON DUPLICATE KEY UPDATE
user_id = VALUES(user_id),
order_id = VALUES(order_id),
txn_type = VALUES(txn_type),
inout_type = VALUES(inout_type),
amount = VALUES(amount),
balance_before = VALUES(balance_before),
balance_after = VALUES(balance_after),
remark = VALUES(remark),
create_time = VALUES(create_time);

-- ---------------------------------------------------------
-- 6) Snapshot
-- ---------------------------------------------------------
INSERT INTO stat_daily_snapshot
(stat_date, instrument_total, instrument_open_total, reservable_instrument_total, machine_order_total, sample_order_total, completed_order_total, effective_work_minutes, external_service_minutes, income_amount, create_time)
VALUES
(CURDATE() - INTERVAL 1 DAY, 2, 2, 2, 3, 1, 1, 120, 0, 240.00, NOW())
ON DUPLICATE KEY UPDATE
instrument_total = VALUES(instrument_total),
instrument_open_total = VALUES(instrument_open_total),
reservable_instrument_total = VALUES(reservable_instrument_total),
machine_order_total = VALUES(machine_order_total),
sample_order_total = VALUES(sample_order_total),
completed_order_total = VALUES(completed_order_total),
effective_work_minutes = VALUES(effective_work_minutes),
external_service_minutes = VALUES(external_service_minutes),
income_amount = VALUES(income_amount);

-- COMMIT;
