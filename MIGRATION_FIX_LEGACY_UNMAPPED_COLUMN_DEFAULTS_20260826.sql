-- =====================================================
-- Migration Script: Fix unmapped legacy score columns
-- Date: 2026-08-26
-- Target DB: performance_management (MySQL 8+)
--
-- Purpose:
--   Current application code no longer maps legacy snapshot columns such
--   as scorecard.agreed_score. If those columns remain in a partially
--   migrated database as NOT NULL with no default, MySQL rejects inserts
--   during scorecard creation/cloning with:
--     Field 'agreed_score' doesn't have a default value
--
--   This compatibility migration preserves existing values and makes any
--   leftover unmapped columns nullable. It is safe to run before the full
--   cleanup migrations that remove the obsolete columns.
-- =====================================================

DROP PROCEDURE IF EXISTS pm_make_legacy_column_nullable;

DELIMITER //
CREATE PROCEDURE pm_make_legacy_column_nullable(
    IN table_name_param VARCHAR(64),
    IN column_name_param VARCHAR(64)
)
BEGIN
    SET @pm_legacy_column_type = NULL;

    SELECT column_type
    INTO @pm_legacy_column_type
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = table_name_param
      AND column_name = column_name_param
    LIMIT 1;

    IF @pm_legacy_column_type IS NOT NULL THEN
        SET @pm_alter_legacy_column_sql = CONCAT(
            'ALTER TABLE `', table_name_param, '` MODIFY COLUMN `',
            column_name_param, '` ', @pm_legacy_column_type, ' NULL'
        );
        PREPARE stmt FROM @pm_alter_legacy_column_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

-- Scorecard fields now live in score/overall_score/overall_comment or
-- workflow stage tables. They are intentionally absent from Scorecard.java.
CALL pm_make_legacy_column_nullable('scorecard', 'model');
CALL pm_make_legacy_column_nullable('scorecard', 'employee_score');
CALL pm_make_legacy_column_nullable('scorecard', 'manager_score');
CALL pm_make_legacy_column_nullable('scorecard', 'agreed_score');
CALL pm_make_legacy_column_nullable('scorecard', 'moderated_score');
CALL pm_make_legacy_column_nullable('scorecard', 'weighted_score');
CALL pm_make_legacy_column_nullable('scorecard', 'owner_comment');
CALL pm_make_legacy_column_nullable('scorecard', 'supervisor_comment');
CALL pm_make_legacy_column_nullable('scorecard', 'moderator_comment');
CALL pm_make_legacy_column_nullable('scorecard', 'approval_status');

-- Target hierarchy and score snapshot helpers are resolved from goal,
-- output, score, evidence, and comments in current code.
CALL pm_make_legacy_column_nullable('target', 'scorecard_id');
CALL pm_make_legacy_column_nullable('target', 'outcome_id');
CALL pm_make_legacy_column_nullable('target', 'gear_id');
CALL pm_make_legacy_column_nullable('target', 'perspective_id');
CALL pm_make_legacy_column_nullable('target', 'strategic_objective_id');
CALL pm_make_legacy_column_nullable('target', 'actual');
CALL pm_make_legacy_column_nullable('target', 'employee_score');
CALL pm_make_legacy_column_nullable('target', 'manager_score');
CALL pm_make_legacy_column_nullable('target', 'agreed_score');
CALL pm_make_legacy_column_nullable('target', 'moderated_score');
CALL pm_make_legacy_column_nullable('target', 'weighted_score');
CALL pm_make_legacy_column_nullable('target', 'current_actual');
CALL pm_make_legacy_column_nullable('target', 'current_employee_score');
CALL pm_make_legacy_column_nullable('target', 'current_manager_score');
CALL pm_make_legacy_column_nullable('target', 'current_agreed_score');
CALL pm_make_legacy_column_nullable('target', 'current_moderated_score');
CALL pm_make_legacy_column_nullable('target', 'current_weighted_score');
CALL pm_make_legacy_column_nullable('target', 'current_evidence');
CALL pm_make_legacy_column_nullable('target', 'current_attachment_name');
CALL pm_make_legacy_column_nullable('target', 'current_justification');

-- Output only maps selected outcome, scorecard, name, allocated weight,
-- and date. Older output-level target/score columns are obsolete.
CALL pm_make_legacy_column_nullable('output', 'unit');
CALL pm_make_legacy_column_nullable('output', 'normal_target');
CALL pm_make_legacy_column_nullable('output', 'base_target');
CALL pm_make_legacy_column_nullable('output', 'stretch_target');
CALL pm_make_legacy_column_nullable('output', 'actual');
CALL pm_make_legacy_column_nullable('output', 'employee_score');
CALL pm_make_legacy_column_nullable('output', 'manager_score');
CALL pm_make_legacy_column_nullable('output', 'agreed_score');
CALL pm_make_legacy_column_nullable('output', 'moderated_score');
CALL pm_make_legacy_column_nullable('output', 'weighted_score');
CALL pm_make_legacy_column_nullable('output', 'current_actual');
CALL pm_make_legacy_column_nullable('output', 'current_employee_score');
CALL pm_make_legacy_column_nullable('output', 'current_manager_score');
CALL pm_make_legacy_column_nullable('output', 'current_agreed_score');
CALL pm_make_legacy_column_nullable('output', 'current_moderated_score');
CALL pm_make_legacy_column_nullable('output', 'current_weighted_score');

DROP PROCEDURE IF EXISTS pm_make_legacy_column_nullable;
