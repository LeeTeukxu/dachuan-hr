-- 员工基本信息：子女信息字段配置
-- 用法：在目标公司库中执行。本脚本幂等，重复执行不会重复插入。
-- 说明：主字段使用 900001，避免旧代码中 hrm_field_extend.parent_field_id 按 Integer 查询时溢出。

START TRANSACTION;

INSERT INTO hrm_employee_field (
  field_id, field_name, name, type, component_type, label, label_group, remark,
  input_tips, max_length, default_value, is_unique, is_null, sorting, options,
  is_fixed, operating, is_hidden, is_update_value, is_head_field, is_import_field,
  is_employee_visible, is_employee_update, style_percent, precisions, form_position,
  max_num_restrict, min_num_restrict, form_assist_id, create_user_id, create_time,
  update_user_id, update_time
)
SELECT
  900001, 'children_info', '子女信息', 45, 0, 1, 1, '添加子女信息',
  '', 255, '[]', 0, 0, 99, '',
  0, '0', 0, 1, 0, 0,
  1, 1, 1, 2, '',
  '', '', NULL, 1, NOW(),
  1, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM hrm_employee_field WHERE field_name = 'children_info' OR field_id = 900001
);

INSERT INTO hrm_field_extend (
  id, parent_field_id, field_name, name, type, remark, input_tips, max_length,
  default_value, is_unique, is_null, sorting, options, operating, is_hidden,
  field_type, style_percent, precisions, form_position, max_num_restrict,
  min_num_restrict, form_assist_id, create_user_id, create_time, update_user_id, update_time
)
SELECT 90000101, 900001, 'childName', '姓名', 1, '', '', 255,
  '', 0, 0, 1, '', 0, 0,
  0, 1, 1, '', '', '', NULL, 1, NOW(), 1, NOW()
WHERE EXISTS (SELECT 1 FROM hrm_employee_field WHERE field_id = 900001 AND field_name = 'children_info')
  AND NOT EXISTS (SELECT 1 FROM hrm_field_extend WHERE parent_field_id = 900001 AND field_name = 'childName');

INSERT INTO hrm_field_extend (
  id, parent_field_id, field_name, name, type, remark, input_tips, max_length,
  default_value, is_unique, is_null, sorting, options, operating, is_hidden,
  field_type, style_percent, precisions, form_position, max_num_restrict,
  min_num_restrict, form_assist_id, create_user_id, create_time, update_user_id, update_time
)
SELECT 90000102, 900001, 'childSex', '性别', 3, '', '', 255,
  '', 0, 0, 2, '[{"name":"男","value":1},{"name":"女","value":2}]', 0, 0,
  0, 1, 1, '', '', '', NULL, 1, NOW(), 1, NOW()
WHERE EXISTS (SELECT 1 FROM hrm_employee_field WHERE field_id = 900001 AND field_name = 'children_info')
  AND NOT EXISTS (SELECT 1 FROM hrm_field_extend WHERE parent_field_id = 900001 AND field_name = 'childSex');

INSERT INTO hrm_field_extend (
  id, parent_field_id, field_name, name, type, remark, input_tips, max_length,
  default_value, is_unique, is_null, sorting, options, operating, is_hidden,
  field_type, style_percent, precisions, form_position, max_num_restrict,
  min_num_restrict, form_assist_id, create_user_id, create_time, update_user_id, update_time
)
SELECT 90000103, 900001, 'childBirthDate', '出生日期', 4, '', '', 255,
  '', 0, 0, 3, '', 0, 0,
  0, 1, 1, '', '', '', NULL, 1, NOW(), 1, NOW()
WHERE EXISTS (SELECT 1 FROM hrm_employee_field WHERE field_id = 900001 AND field_name = 'children_info')
  AND NOT EXISTS (SELECT 1 FROM hrm_field_extend WHERE parent_field_id = 900001 AND field_name = 'childBirthDate');

COMMIT;
