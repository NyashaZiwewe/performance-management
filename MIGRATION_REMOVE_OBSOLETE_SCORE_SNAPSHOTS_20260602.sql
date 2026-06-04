-- =====================================================
-- Migration Script: Remove obsolete score snapshot columns
-- Date: 2026-06-02
-- Target DB: performance_management (MySQL 8+)
--
-- Assumption:
--   MIGRATION_MINIMAL_ALIGNMENT_20260601.sql has already run, so
--   reporting_period.model is the hierarchy driver and score rows are
--   the source of truth for per-reporting-date score/evidence data.
-- =====================================================

DROP PROCEDURE IF EXISTS drop_pm_column_if_exists;

DELIMITER //
CREATE PROCEDURE drop_pm_column_if_exists(
    IN table_name_param VARCHAR(64),
    IN column_name_param VARCHAR(64)
)
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE fk_name VARCHAR(64);
    DECLARE fk_cursor CURSOR FOR
        SELECT constraint_name
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE table_schema = DATABASE()
          AND table_name = table_name_param
          AND column_name = column_name_param
          AND referenced_table_name IS NOT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN fk_cursor;
    fk_loop: LOOP
        FETCH fk_cursor INTO fk_name;
        IF done THEN
            LEAVE fk_loop;
        END IF;

        SET @drop_fk_sql = CONCAT(
            'ALTER TABLE `', table_name_param, '` DROP FOREIGN KEY `', fk_name, '`'
        );
        PREPARE stmt FROM @drop_fk_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE fk_cursor;

    SET @column_exists = (
        SELECT COUNT(*)
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_param
          AND column_name = column_name_param
    );

    IF @column_exists > 0 THEN
        SET @drop_col_sql = CONCAT(
            'ALTER TABLE `', table_name_param, '` DROP COLUMN `', column_name_param, '`'
        );
        PREPARE stmt FROM @drop_col_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

-- scorecard.model moved to reporting_period.model.
CALL drop_pm_column_if_exists('scorecard', 'model');

-- Target hierarchy display helpers are now transient and resolved from
-- target.output -> output.outcome -> goal/pillar.
CALL drop_pm_column_if_exists('target', 'outcome_id');
CALL drop_pm_column_if_exists('target', 'gear_id');
CALL drop_pm_column_if_exists('target', 'perspective_id');
CALL drop_pm_column_if_exists('target', 'strategic_objective_id');

-- Target aggregate/current score columns duplicated score rows.
CALL drop_pm_column_if_exists('target', 'actual');
CALL drop_pm_column_if_exists('target', 'employee_score');
CALL drop_pm_column_if_exists('target', 'manager_score');
CALL drop_pm_column_if_exists('target', 'agreed_score');
CALL drop_pm_column_if_exists('target', 'moderated_score');
CALL drop_pm_column_if_exists('target', 'weighted_score');
CALL drop_pm_column_if_exists('target', 'current_actual');
CALL drop_pm_column_if_exists('target', 'current_employee_score');
CALL drop_pm_column_if_exists('target', 'current_manager_score');
CALL drop_pm_column_if_exists('target', 'current_agreed_score');
CALL drop_pm_column_if_exists('target', 'current_moderated_score');
CALL drop_pm_column_if_exists('target', 'current_weighted_score');
CALL drop_pm_column_if_exists('target', 'current_evidence');
CALL drop_pm_column_if_exists('target', 'current_attachment_name');
CALL drop_pm_column_if_exists('target', 'current_justification');

-- Output only needs the selected outcome, scorecard, name, and allocated
-- weight. Target thresholds and score snapshots belong to target/score.
CALL drop_pm_column_if_exists('output', 'unit');
CALL drop_pm_column_if_exists('output', 'normal_target');
CALL drop_pm_column_if_exists('output', 'base_target');
CALL drop_pm_column_if_exists('output', 'stretch_target');
CALL drop_pm_column_if_exists('output', 'actual');
CALL drop_pm_column_if_exists('output', 'employee_score');
CALL drop_pm_column_if_exists('output', 'manager_score');
CALL drop_pm_column_if_exists('output', 'agreed_score');
CALL drop_pm_column_if_exists('output', 'moderated_score');
CALL drop_pm_column_if_exists('output', 'weighted_score');
CALL drop_pm_column_if_exists('output', 'current_actual');
CALL drop_pm_column_if_exists('output', 'current_employee_score');
CALL drop_pm_column_if_exists('output', 'current_manager_score');
CALL drop_pm_column_if_exists('output', 'current_agreed_score');
CALL drop_pm_column_if_exists('output', 'current_moderated_score');
CALL drop_pm_column_if_exists('output', 'current_weighted_score');

DROP PROCEDURE IF EXISTS drop_pm_column_if_exists;
