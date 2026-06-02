-- =====================================================
-- Migration Script: Minimal Production Alignment
-- Date: 2026-06-01
-- Target DB: performance_management (MySQL 8+)
--
-- Goal:
-- 1) Keep changes additive and low-risk.
-- 2) Align existing tables used by current code paths.
-- 3) Create missing tables for new probation flow and supporting modules.
-- =====================================================

-- -----------------------------------------------------
-- SECTION A: Existing table alignment (minimal/inevitable)
-- -----------------------------------------------------

-- reporting_period.model is now used as the hierarchy driver
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'reporting_period' AND column_name = 'model'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE reporting_period ADD COLUMN model VARCHAR(50) DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill from scorecard.model when available (only if scorecard.model still exists)
SET @scorecard_model_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'scorecard' AND column_name = 'model'
);
SET @sql = IF(
    @scorecard_model_exists > 0,
    'UPDATE reporting_period rp LEFT JOIN (SELECT reporting_period_id, MAX(NULLIF(model, '''')) AS migrated_model FROM scorecard GROUP BY reporting_period_id) sc ON sc.reporting_period_id = rp.id SET rp.model = COALESCE(rp.model, sc.migrated_model, ''standard'') WHERE rp.model IS NULL OR rp.model = ''''',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Safety default
UPDATE reporting_period
SET model = 'standard'
WHERE model IS NULL OR model = '';

-- -----------------------------------------------------
-- Scorecard workflow stage normalization and linkage
-- -----------------------------------------------------

CREATE TABLE IF NOT EXISTS scorecard_workflow_stage (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_id BIGINT DEFAULT NULL,
    name VARCHAR(255) DEFAULT NULL,
    stage_order INT DEFAULT NULL,
    role_key VARCHAR(255) DEFAULT NULL,
    status_code VARCHAR(255) DEFAULT NULL,
    status_codes VARCHAR(1000) DEFAULT NULL,
    status_label VARCHAR(255) DEFAULT NULL,
    action_button_label VARCHAR(255) DEFAULT NULL,
    rejection_button_label VARCHAR(255) DEFAULT NULL,
    status VARCHAR(255) DEFAULT NULL,
    date_created DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'scorecard_workflow_stage' AND column_name = 'status_codes'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE scorecard_workflow_stage ADD COLUMN status_codes VARCHAR(1000) DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'scorecard' AND column_name = 'approval_stage_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE scorecard ADD COLUMN approval_stage_id BIGINT DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'scorecard' AND index_name = 'idx_scorecard_approval_stage_id'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_scorecard_approval_stage_id ON scorecard(approval_stage_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Pass 1: direct status-code matching (status_code or status_codes CSV).
UPDATE scorecard s
JOIN (
    SELECT s2.id AS scorecard_id, MIN(st.id) AS stage_id
    FROM scorecard s2
    JOIN scorecard_workflow_stage st
      ON st.client_id = s2.client_id
     AND UPPER(COALESCE(st.status, 'ACTIVE')) = 'ACTIVE'
     AND (
            UPPER(COALESCE(st.status_code, '')) = UPPER(COALESCE(s2.approval_status, ''))
         OR UPPER(CONCAT(',', REPLACE(COALESCE(st.status_codes, ''), ' ', ''), ','))
            LIKE UPPER(CONCAT('%,', REPLACE(COALESCE(s2.approval_status, ''), ' ', ''), ',%'))
     )
    WHERE s2.approval_stage_id IS NULL OR s2.approval_stage_id = 0
    GROUP BY s2.id
) mapped ON mapped.scorecard_id = s.id
SET s.approval_stage_id = mapped.stage_id
WHERE s.approval_stage_id IS NULL OR s.approval_stage_id = 0;

-- Pass 2: infer stage by stage-role mapping when direct status-code matching is unavailable.
-- Special mappings requested:
-- MODERATED_BY_HR -> SCORECARD_STAGE_CLOSED
-- APPROVED_BY_SUPERVISOR -> SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR
UPDATE scorecard s
JOIN (
    SELECT s2.id AS scorecard_id, MIN(st.id) AS stage_id
    FROM scorecard s2
    JOIN scorecard_workflow_stage st
      ON st.client_id = s2.client_id
     AND UPPER(COALESCE(st.status, 'ACTIVE')) = 'ACTIVE'
     AND UPPER(COALESCE(st.role_key, '')) = (
         CASE UPPER(COALESCE(s2.approval_status, ''))
             WHEN 'NEW' THEN 'SCORECARD_STAGE_NEW'
             WHEN 'PENDING_APPROVAL' THEN 'SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR'
             WHEN 'APPROVED_BY_SUPERVISOR' THEN 'SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR'
             WHEN 'REJECTED_BY_SUPERVISOR' THEN 'SCORECARD_STAGE_CAPTURE_TARGETS'
             WHEN 'APPROVED_BY_HR' THEN 'SCORECARD_STAGE_OWNER_SCORING'
             WHEN 'REJECTED_BY_HR' THEN 'SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR'
             WHEN 'SCORED_BY_EMPLOYEE' THEN 'SCORECARD_STAGE_OWNER_SCORE_APPROVAL'
             WHEN 'APPROVED_OWNER_SCORES' THEN 'SCORECARD_STAGE_SUPERVISOR_SCORING'
             WHEN 'SCORED_BY_SUPERVISOR' THEN 'SCORECARD_STAGE_AGREED_SCORE_CAPTURING'
             WHEN 'AGREED_BY_TWO' THEN 'SCORECARD_STAGE_AGREED_SCORE_APPROVAL'
             WHEN 'APPROVED_AGREED_SCORES' THEN 'SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING'
             WHEN 'MODERATED_BY_HR' THEN 'SCORECARD_STAGE_CLOSED'
             WHEN 'CLOSED' THEN 'SCORECARD_STAGE_CLOSED'
             WHEN 'PENDING' THEN 'SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR'
             WHEN 'APPROVED' THEN 'SCORECARD_STAGE_OWNER_SCORING'
             WHEN 'REJECTED' THEN 'SCORECARD_STAGE_CAPTURE_TARGETS'
             WHEN 'RETURNED' THEN 'SCORECARD_STAGE_CAPTURE_TARGETS'
             ELSE ''
         END
     )
    WHERE s2.approval_stage_id IS NULL OR s2.approval_stage_id = 0
    GROUP BY s2.id
) mapped ON mapped.scorecard_id = s.id
SET s.approval_stage_id = mapped.stage_id
WHERE s.approval_stage_id IS NULL OR s.approval_stage_id = 0;

-- Ensure no orphan stage references before adding FK.
UPDATE scorecard s
LEFT JOIN scorecard_workflow_stage st ON st.id = s.approval_stage_id
SET s.approval_stage_id = NULL
WHERE s.approval_stage_id IS NOT NULL
  AND st.id IS NULL;

SET @expected_stage_fk = 'fk_scorecard_approval_stage';
SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE table_schema = DATABASE()
      AND table_name = 'scorecard'
      AND constraint_name = @expected_stage_fk
      AND column_name = 'approval_stage_id'
      AND referenced_table_name = 'scorecard_workflow_stage'
);

SET @existing_fk = (
    SELECT MIN(constraint_name)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE table_schema = DATABASE()
      AND table_name = 'scorecard'
      AND column_name = 'approval_stage_id'
      AND referenced_table_name = 'scorecard_workflow_stage'
);

SET @sql = IF(
    @fk_exists = 0 AND @existing_fk IS NOT NULL,
    CONCAT('ALTER TABLE scorecard DROP FOREIGN KEY `', @existing_fk, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @fk_exists = 0,
    CONCAT('ALTER TABLE scorecard ADD CONSTRAINT `', @expected_stage_fk, '` FOREIGN KEY (approval_stage_id) REFERENCES scorecard_workflow_stage(id)'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------
-- Scorecard stage per reporting date (scoring workflow isolation)
-- -----------------------------------------------------

CREATE TABLE IF NOT EXISTS scorecard_reporting_date_stage (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_id BIGINT DEFAULT NULL,
    scorecard_id BIGINT DEFAULT NULL,
    reporting_date_id BIGINT DEFAULT NULL,
    approval_stage_id BIGINT DEFAULT NULL,
    status VARCHAR(255) DEFAULT NULL,
    date_created DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_srds_scorecard_id (scorecard_id),
    KEY idx_srds_reporting_date_id (reporting_date_id),
    KEY idx_srds_approval_stage_id (approval_stage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Backfill missing scorecard/reporting-date rows.
INSERT INTO scorecard_reporting_date_stage (client_id, scorecard_id, reporting_date_id, status)
SELECT s.client_id, s.id, rd.id, 'ACTIVE'
FROM scorecard s
JOIN reporting_date rd ON rd.reporting_period_id = s.reporting_period_id
LEFT JOIN scorecard_reporting_date_stage sr
       ON sr.scorecard_id = s.id
      AND sr.reporting_date_id = rd.id
WHERE sr.id IS NULL;

-- Deduplicate pair rows before enforcing uniqueness.
DELETE sr1
FROM scorecard_reporting_date_stage sr1
JOIN scorecard_reporting_date_stage sr2
  ON sr1.scorecard_id = sr2.scorecard_id
 AND sr1.reporting_date_id = sr2.reporting_date_id
 AND sr1.id > sr2.id;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'scorecard_reporting_date_stage'
      AND index_name = 'uk_scorecard_reporting_date_stage'
);
SET @sql = IF(
    @idx_exists = 0,
    'CREATE UNIQUE INDEX uk_scorecard_reporting_date_stage ON scorecard_reporting_date_stage(scorecard_id, reporting_date_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Set by score capture progression first (highest completed stage wins).
UPDATE scorecard_reporting_date_stage sr
JOIN (
    SELECT sr2.id AS sr_id,
           CASE
               WHEN SUM(CASE WHEN sc.moderated_score >= 1 AND sc.moderated_score <= 5 THEN 1 ELSE 0 END) > 0 THEN 'SCORECARD_STAGE_CLOSED'
               WHEN SUM(CASE WHEN sc.agreed_score >= 1 AND sc.agreed_score <= 5 THEN 1 ELSE 0 END) > 0 THEN 'SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING'
               WHEN SUM(CASE WHEN sc.manager_score >= 1 AND sc.manager_score <= 5 THEN 1 ELSE 0 END) > 0 THEN 'SCORECARD_STAGE_AGREED_SCORE_CAPTURING'
               WHEN SUM(CASE WHEN sc.employee_score >= 1 AND sc.employee_score <= 5 THEN 1 ELSE 0 END) > 0 THEN 'SCORECARD_STAGE_OWNER_SCORE_APPROVAL'
               ELSE NULL
           END AS role_key
    FROM scorecard_reporting_date_stage sr2
    JOIN scorecard s2 ON s2.id = sr2.scorecard_id
    LEFT JOIN score sc
           ON sc.reporting_date_id = sr2.reporting_date_id
    LEFT JOIN target t ON t.id = sc.target_id
    LEFT JOIN goal g ON g.id = t.goal_id
    LEFT JOIN output o ON o.id = sc.output_id
    WHERE (g.scorecard_id = s2.id OR o.scorecard_id = s2.id)
    GROUP BY sr2.id
) progress ON progress.sr_id = sr.id
JOIN scorecard s ON s.id = sr.scorecard_id
JOIN scorecard_workflow_stage st
  ON st.client_id = s.client_id
 AND UPPER(COALESCE(st.status, 'ACTIVE')) = 'ACTIVE'
 AND UPPER(COALESCE(st.role_key, '')) = progress.role_key
SET sr.approval_stage_id = st.id,
    sr.status = COALESCE(sr.status, 'ACTIVE')
WHERE progress.role_key IS NOT NULL;

-- Fallback to global scorecard status for rows still not mapped.
UPDATE scorecard_reporting_date_stage sr
JOIN scorecard s ON s.id = sr.scorecard_id
JOIN scorecard_workflow_stage st
  ON st.client_id = s.client_id
 AND UPPER(COALESCE(st.status, 'ACTIVE')) = 'ACTIVE'
 AND UPPER(COALESCE(st.role_key, '')) = (
     CASE UPPER(COALESCE(s.approval_status, ''))
         WHEN 'APPROVED_BY_HR' THEN 'SCORECARD_STAGE_OWNER_SCORING'
         WHEN 'SCORED_BY_EMPLOYEE' THEN 'SCORECARD_STAGE_OWNER_SCORE_APPROVAL'
         WHEN 'APPROVED_OWNER_SCORES' THEN 'SCORECARD_STAGE_SUPERVISOR_SCORING'
         WHEN 'SCORED_BY_SUPERVISOR' THEN 'SCORECARD_STAGE_AGREED_SCORE_CAPTURING'
         WHEN 'AGREED_BY_TWO' THEN 'SCORECARD_STAGE_AGREED_SCORE_APPROVAL'
         WHEN 'APPROVED_AGREED_SCORES' THEN 'SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING'
         WHEN 'MODERATED_BY_HR' THEN 'SCORECARD_STAGE_CLOSED'
         WHEN 'CLOSED' THEN 'SCORECARD_STAGE_CLOSED'
         ELSE 'SCORECARD_STAGE_OWNER_SCORING'
     END
 )
SET sr.approval_stage_id = st.id,
    sr.status = COALESCE(sr.status, 'ACTIVE')
WHERE sr.approval_stage_id IS NULL;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE table_schema = DATABASE()
      AND table_name = 'scorecard_reporting_date_stage'
      AND constraint_name = 'fk_srds_scorecard'
      AND column_name = 'scorecard_id'
      AND referenced_table_name = 'scorecard'
);
SET @sql = IF(
    @fk_exists = 0,
    'ALTER TABLE scorecard_reporting_date_stage ADD CONSTRAINT fk_srds_scorecard FOREIGN KEY (scorecard_id) REFERENCES scorecard(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE table_schema = DATABASE()
      AND table_name = 'scorecard_reporting_date_stage'
      AND constraint_name = 'fk_srds_reporting_date'
      AND column_name = 'reporting_date_id'
      AND referenced_table_name = 'reporting_date'
);
SET @sql = IF(
    @fk_exists = 0,
    'ALTER TABLE scorecard_reporting_date_stage ADD CONSTRAINT fk_srds_reporting_date FOREIGN KEY (reporting_date_id) REFERENCES reporting_date(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE table_schema = DATABASE()
      AND table_name = 'scorecard_reporting_date_stage'
      AND constraint_name = 'fk_srds_approval_stage'
      AND column_name = 'approval_stage_id'
      AND referenced_table_name = 'scorecard_workflow_stage'
);
SET @sql = IF(
    @fk_exists = 0,
    'ALTER TABLE scorecard_reporting_date_stage ADD CONSTRAINT fk_srds_approval_stage FOREIGN KEY (approval_stage_id) REFERENCES scorecard_workflow_stage(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Predefined metrics are now optionally scoped to reporting period
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'gear' AND column_name = 'reporting_period_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE gear ADD COLUMN reporting_period_id BIGINT DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Goal now carries optional scorecard/perspective/strategic objective links
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'goal' AND column_name = 'scorecard_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE goal ADD COLUMN scorecard_id BIGINT DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'goal' AND column_name = 'perspective_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE goal ADD COLUMN perspective_id BIGINT DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'goal' AND column_name = 'strategic_objective_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE goal ADD COLUMN strategic_objective_id BIGINT DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Keep legacy/global goals compatible with "not linked to scorecard"
UPDATE goal
SET scorecard_id = 0
WHERE scorecard_id IS NULL;

-- Target now optionally carries a direct goal reference for faster scorecard joins.
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'target' AND column_name = 'goal_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE target ADD COLUMN goal_id BIGINT DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill goal_id using existing hierarchy:
-- target -> output -> outcome -> (goal or pillar.goal)
UPDATE target t
JOIN output o ON o.id = t.output_id
LEFT JOIN outcome oc ON oc.id = o.outcome_id
LEFT JOIN pillar p ON p.id = oc.pillar_id
SET t.goal_id = COALESCE(oc.goal_id, p.goal_id)
WHERE (t.goal_id IS NULL OR t.goal_id = 0)
  AND COALESCE(oc.goal_id, p.goal_id) IS NOT NULL;

-- Backup unresolved targets for manual cleanup (non-destructive; idempotent via PK id + INSERT IGNORE).
CREATE TABLE IF NOT EXISTS target_fk_repair_backup LIKE target;
INSERT IGNORE INTO target_fk_repair_backup
SELECT t.*
FROM target t
LEFT JOIN output o ON o.id = t.output_id
LEFT JOIN goal g ON g.id = t.goal_id
WHERE (t.output_id IS NOT NULL AND o.id IS NULL)
   OR t.goal_id IS NULL
   OR t.goal_id = 0
   OR g.id IS NULL;

-- Optional lightweight indexes (created only if missing)
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'gear' AND index_name = 'idx_gear_reporting_period_id'
);
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_gear_reporting_period_id ON gear(reporting_period_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'goal' AND index_name = 'idx_goal_scorecard_id'
);
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_goal_scorecard_id ON goal(scorecard_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'goal' AND index_name = 'idx_goal_perspective_id'
);
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_goal_perspective_id ON goal(perspective_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'goal' AND index_name = 'idx_goal_strategic_objective_id'
);
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_goal_strategic_objective_id ON goal(strategic_objective_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'target' AND index_name = 'idx_target_goal_id'
);
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_target_goal_id ON target(goal_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Deliberately avoid adding `target.goal_id` / `target.output_id` FK constraints here.
-- Legacy production datasets can contain historical orphan references; forcing FKs causes startup failures.
-- The application resolves score/target ownership via output hierarchy and remains compatible without these constraints.

-- -----------------------------------------------------
-- SECTION B: Strategic hierarchy tables (required)
-- -----------------------------------------------------

CREATE TABLE IF NOT EXISTS perspective (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_id BIGINT DEFAULT NULL,
    name VARCHAR(255) DEFAULT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    graph_color VARCHAR(255) DEFAULT NULL,
    fill VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS strategic_objective (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporting_period_id BIGINT DEFAULT NULL,
    name VARCHAR(500) DEFAULT NULL,
    date_added DATE DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_strategic_objective_reporting_period_id (reporting_period_id),
    CONSTRAINT fk_strategic_objective_reporting_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_period (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Existing strategic_objective tables (if any) are aligned column-by-column.
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'strategic_objective' AND column_name = 'reporting_period_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE strategic_objective ADD COLUMN reporting_period_id BIGINT DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'strategic_objective' AND column_name = 'name'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE strategic_objective ADD COLUMN name VARCHAR(500) DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'strategic_objective' AND column_name = 'date_added'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE strategic_objective ADD COLUMN date_added DATE DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'strategic_objective' AND index_name = 'idx_strategic_objective_reporting_period_id'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_strategic_objective_reporting_period_id ON strategic_objective(reporting_period_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------
-- SECTION C: Probation assessment tables (new feature)
-- -----------------------------------------------------

CREATE TABLE IF NOT EXISTS probation_assessment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_id BIGINT DEFAULT NULL,
    employee_id BIGINT DEFAULT NULL,
    performance_period VARCHAR(255) DEFAULT NULL,
    start_date VARCHAR(255) DEFAULT NULL,
    end_date VARCHAR(255) DEFAULT NULL,
    general_observations VARCHAR(3000) DEFAULT NULL,
    employee_comment VARCHAR(3000) DEFAULT NULL,
    supervisor_comment VARCHAR(3000) DEFAULT NULL,
    status VARCHAR(255) DEFAULT NULL,
    current_step_order INT DEFAULT NULL,
    current_step_name VARCHAR(255) DEFAULT NULL,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_probation_assessment_client (client_id),
    KEY idx_probation_assessment_employee (employee_id),
    KEY idx_probation_assessment_status (status),
    CONSTRAINT fk_probation_assessment_employee
        FOREIGN KEY (employee_id) REFERENCES account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS probation_dimension_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_id BIGINT DEFAULT NULL,
    code VARCHAR(255) DEFAULT NULL,
    title VARCHAR(255) DEFAULT NULL,
    description VARCHAR(3000) DEFAULT NULL,
    display_order INT DEFAULT NULL,
    status VARCHAR(255) DEFAULT NULL,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_probation_dimension_template_client (client_id),
    KEY idx_probation_dimension_template_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS probation_workflow_step (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_id BIGINT DEFAULT NULL,
    name VARCHAR(255) DEFAULT NULL,
    step_order INT DEFAULT NULL,
    approver_mode VARCHAR(255) DEFAULT NULL,
    approver_account_type VARCHAR(255) DEFAULT NULL,
    approver_role VARCHAR(255) DEFAULT NULL,
    same_division_only BIT(1) DEFAULT NULL,
    status VARCHAR(255) DEFAULT NULL,
    approver_account_id BIGINT DEFAULT NULL,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_probation_workflow_step_client (client_id),
    KEY idx_probation_workflow_step_status (status),
    KEY idx_probation_workflow_step_approver (approver_account_id),
    CONSTRAINT fk_probation_workflow_step_approver
        FOREIGN KEY (approver_account_id) REFERENCES account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS probation_assessment_dimension (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assessment_id BIGINT DEFAULT NULL,
    dimension_template_id BIGINT DEFAULT NULL,
    strengths VARCHAR(3000) DEFAULT NULL,
    areas_for_improvement VARCHAR(3000) DEFAULT NULL,
    performance_improvement_plan_id BIGINT DEFAULT NULL,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_probation_dimension_assessment_template (assessment_id, dimension_template_id),
    KEY idx_probation_assessment_dimension_pip (performance_improvement_plan_id),
    CONSTRAINT fk_probation_assessment_dimension_assessment
        FOREIGN KEY (assessment_id) REFERENCES probation_assessment (id),
    CONSTRAINT fk_probation_assessment_dimension_template
        FOREIGN KEY (dimension_template_id) REFERENCES probation_dimension_template (id),
    CONSTRAINT fk_probation_assessment_dimension_pip
        FOREIGN KEY (performance_improvement_plan_id) REFERENCES performance_improvement_plan (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS probation_kpi (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assessment_id BIGINT DEFAULT NULL,
    name VARCHAR(255) DEFAULT NULL,
    measure_of_success VARCHAR(1000) DEFAULT NULL,
    target VARCHAR(1000) DEFAULT NULL,
    incumbent_mark DOUBLE DEFAULT NULL,
    supervisor_mark DOUBLE DEFAULT NULL,
    progress_percent DOUBLE DEFAULT NULL,
    progress_comment VARCHAR(3000) DEFAULT NULL,
    incumbent_comment VARCHAR(3000) DEFAULT NULL,
    supervisor_comment VARCHAR(3000) DEFAULT NULL,
    attachment_path VARCHAR(1000) DEFAULT NULL,
    flag VARCHAR(2000) DEFAULT NULL,
    status VARCHAR(255) DEFAULT NULL,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_probation_kpi_assessment (assessment_id),
    CONSTRAINT fk_probation_kpi_assessment
        FOREIGN KEY (assessment_id) REFERENCES probation_assessment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS probation_kpi_comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    probation_kpi_id BIGINT DEFAULT NULL,
    sender_id BIGINT DEFAULT NULL,
    message VARCHAR(3000) DEFAULT NULL,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_probation_kpi_comment_kpi (probation_kpi_id),
    KEY idx_probation_kpi_comment_sender (sender_id),
    CONSTRAINT fk_probation_kpi_comment_kpi
        FOREIGN KEY (probation_kpi_id) REFERENCES probation_kpi (id),
    CONSTRAINT fk_probation_kpi_comment_sender
        FOREIGN KEY (sender_id) REFERENCES account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS probation_assessment_approval (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assessment_id BIGINT DEFAULT NULL,
    workflow_step_name VARCHAR(255) DEFAULT NULL,
    step_order INT DEFAULT NULL,
    action VARCHAR(255) DEFAULT NULL,
    actor_id BIGINT DEFAULT NULL,
    remarks VARCHAR(3000) DEFAULT NULL,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_probation_assessment_approval_assessment (assessment_id),
    KEY idx_probation_assessment_approval_actor (actor_id),
    CONSTRAINT fk_probation_assessment_approval_assessment
        FOREIGN KEY (assessment_id) REFERENCES probation_assessment (id),
    CONSTRAINT fk_probation_assessment_approval_actor
        FOREIGN KEY (actor_id) REFERENCES account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- -----------------------------------------------------
-- SECTION D: Sanity checks
-- -----------------------------------------------------
-- 1) SELECT id, start_date, end_date, model FROM reporting_period;
-- 2) SHOW COLUMNS FROM gear LIKE 'reporting_period_id';
-- 3) SHOW COLUMNS FROM goal LIKE 'scorecard_id';
-- 4) SHOW TABLES LIKE 'probation_%';
