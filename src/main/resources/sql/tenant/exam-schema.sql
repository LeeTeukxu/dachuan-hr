-- =============================================
-- 培训考试系统数据库迁移脚本
-- 创建时间：2026-09-02
-- =============================================

-- 1. 员工表新增字段（考试相关）
ALTER TABLE hrm_employee ADD COLUMN exam_total_score decimal(10,2) DEFAULT 0 COMMENT '考试累计总分';
ALTER TABLE hrm_employee ADD COLUMN exam_count int DEFAULT 0 COMMENT '考试次数';
ALTER TABLE hrm_employee ADD COLUMN exam_avg_score decimal(10,2) DEFAULT 0 COMMENT '考试平均分';

-- 2. 试卷表
CREATE TABLE IF NOT EXISTS exam_paper (
  paper_id bigint NOT NULL AUTO_INCREMENT,
  paper_name varchar(255) NOT NULL COMMENT '试卷名称',
  total_score decimal(10,2) DEFAULT 0 COMMENT '总分',
  duration int DEFAULT 60 COMMENT '考试时长（分钟）',
  pass_score decimal(10,2) DEFAULT 0 COMMENT '及格分',
  status int DEFAULT 1 COMMENT '1草稿 2已发布 3已结束',
  created_by bigint COMMENT '创建人ID',
  created_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (paper_id),
  KEY idx_paper_status (status),
  KEY idx_paper_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='试卷表';

-- 3. 题目表
CREATE TABLE IF NOT EXISTS exam_question (
  question_id bigint NOT NULL AUTO_INCREMENT,
  paper_id bigint NOT NULL COMMENT '试卷ID',
  question_type int NOT NULL COMMENT '1单选 2多选 3填空 4简答',
  content text NOT NULL COMMENT '题目内容',
  options json COMMENT '选项JSON（选择题用）',
  correct_answer text COMMENT '正确答案（选择题自动判分用）',
  score decimal(10,2) NOT NULL COMMENT '题目分值',
  sort_order int DEFAULT 0 COMMENT '排序',
  created_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (question_id),
  KEY idx_question_paper_id (paper_id),
  KEY idx_question_type (question_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='题目表';

-- 4. 培训资料表（全局共享）
CREATE TABLE IF NOT EXISTS exam_material (
  material_id bigint NOT NULL AUTO_INCREMENT,
  material_name varchar(255) NOT NULL COMMENT '资料名称',
  file_url varchar(500) NOT NULL COMMENT '文件URL',
  file_type varchar(50) COMMENT '文件类型（pdf/doc/xls/ppt/mp4等）',
  file_size bigint COMMENT '文件大小（字节）',
  uploader_id bigint COMMENT '上传人ID',
  upload_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (material_id),
  KEY idx_material_type (file_type),
  KEY idx_material_uploader (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='培训资料表';

-- 5. 培训资料访问记录表（水印追溯）
CREATE TABLE IF NOT EXISTS exam_material_log (
  log_id bigint NOT NULL AUTO_INCREMENT,
  material_id bigint NOT NULL COMMENT '资料ID',
  employee_id bigint NOT NULL COMMENT '员工ID',
  view_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '查看时间',
  ip_address varchar(50) COMMENT 'IP地址',
  PRIMARY KEY (log_id),
  KEY idx_log_material_id (material_id),
  KEY idx_log_employee_id (employee_id),
  KEY idx_log_view_time (view_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='培训资料访问记录表';

-- 6. 考试发布表
CREATE TABLE IF NOT EXISTS exam_assignment (
  assignment_id bigint NOT NULL AUTO_INCREMENT,
  paper_id bigint NOT NULL COMMENT '试卷ID',
  assignment_name varchar(255) COMMENT '考试名称',
  start_time datetime COMMENT '开始时间',
  end_time datetime COMMENT '截止时间',
  status int DEFAULT 1 COMMENT '1草稿 2已发布 3已结束',
  created_by bigint COMMENT '创建人ID',
  created_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (assignment_id),
  KEY idx_assignment_paper_id (paper_id),
  KEY idx_assignment_status (status),
  KEY idx_assignment_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试发布表';

-- 7. 考试关联培训资料表
CREATE TABLE IF NOT EXISTS exam_assignment_material (
  id bigint NOT NULL AUTO_INCREMENT,
  assignment_id bigint NOT NULL COMMENT '考试发布ID',
  material_id bigint NOT NULL COMMENT '培训资料ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_assignment_material (assignment_id, material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试关联培训资料表';

-- 8. 考试指定人员表
CREATE TABLE IF NOT EXISTS exam_assignment_target (
  id bigint NOT NULL AUTO_INCREMENT,
  assignment_id bigint NOT NULL COMMENT '考试发布ID',
  target_type int NOT NULL COMMENT '1按部门 2按员工',
  target_id bigint NOT NULL COMMENT '部门ID或员工ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_assignment_target (assignment_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试指定人员表';

-- 9. 阅卷人配置表
CREATE TABLE IF NOT EXISTS exam_grader_config (
  config_id bigint NOT NULL AUTO_INCREMENT,
  assignment_id bigint NOT NULL COMMENT '考试发布ID',
  grader_type int NOT NULL COMMENT '1按部门 2按员工',
  target_id bigint NOT NULL COMMENT '部门ID或员工ID',
  grader_id bigint NOT NULL COMMENT '阅卷人ID',
  created_by bigint COMMENT '创建人ID',
  created_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (config_id),
  UNIQUE KEY uk_assignment_grader (assignment_id, grader_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='阅卷人配置表';

-- 10. 考试记录表（支持多次考试）
CREATE TABLE IF NOT EXISTS exam_record (
  record_id bigint NOT NULL AUTO_INCREMENT,
  assignment_id bigint NOT NULL COMMENT '考试发布ID',
  paper_id bigint NOT NULL COMMENT '试卷ID',
  employee_id bigint NOT NULL COMMENT '员工ID',
  attempt_number int DEFAULT 1 COMMENT '第几次考试',
  start_time datetime COMMENT '开始时间',
  submit_time datetime COMMENT '交卷时间',
  status int DEFAULT 1 COMMENT '1进行中 2已交卷 3已阅卷',
  total_score decimal(10,2) DEFAULT 0 COMMENT '总分',
  grader_id bigint COMMENT '阅卷人ID',
  graded_time datetime COMMENT '阅卷时间',
  is_late tinyint DEFAULT 0 COMMENT '是否迟交（0否 1是）',
  PRIMARY KEY (record_id),
  UNIQUE KEY uk_assignment_employee_attempt (assignment_id, employee_id, attempt_number),
  KEY idx_record_assignment_id (assignment_id),
  KEY idx_record_employee_id (employee_id),
  KEY idx_record_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试记录表';

-- 11. 答题表
CREATE TABLE IF NOT EXISTS exam_answer (
  answer_id bigint NOT NULL AUTO_INCREMENT,
  record_id bigint NOT NULL COMMENT '考试记录ID',
  question_id bigint NOT NULL COMMENT '题目ID',
  employee_answer text COMMENT '员工答案',
  is_correct int COMMENT '是否正确（1正确 0错误，选择题用）',
  score decimal(10,2) DEFAULT 0 COMMENT '得分',
  graded_by bigint COMMENT '阅卷人ID',
  graded_time datetime COMMENT '阅卷时间',
  comment text COMMENT '阅卷备注（简答题用）',
  PRIMARY KEY (answer_id),
  UNIQUE KEY uk_record_question (record_id, question_id),
  KEY idx_answer_record_id (record_id),
  KEY idx_answer_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='答题表';

-- 12. 通知表
CREATE TABLE IF NOT EXISTS exam_notification (
  notification_id bigint NOT NULL AUTO_INCREMENT,
  employee_id bigint NOT NULL COMMENT '员工ID',
  title varchar(255) NOT NULL COMMENT '通知标题',
  content text COMMENT '通知内容',
  notification_type int COMMENT '1考试通知 2成绩通知 3系统通知',
  related_id bigint COMMENT '关联ID（如考试ID）',
  is_read tinyint DEFAULT 0 COMMENT '是否已读（0未读 1已读）',
  created_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  read_time datetime COMMENT '阅读时间',
  PRIMARY KEY (notification_id),
  KEY idx_notification_employee_id (employee_id),
  KEY idx_notification_is_read (is_read),
  KEY idx_notification_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知表';
