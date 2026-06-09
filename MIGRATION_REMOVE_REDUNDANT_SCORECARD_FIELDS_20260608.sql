-- =====================================================
-- Migration Script: Remove redundant scorecard snapshots
-- Date: 2026-06-08
-- Target DB: performance_management (MySQL 8+)
--
-- Run after MIGRATION_MINIMAL_ALIGNMENT_20260601.sql.
-- Score, overall_score, overall_comment, and approval_stage_id are the
-- authoritative reporting-date/workflow records after this migration.
-- This is a one-time migration because it references the legacy columns
-- before dropping them.
-- =====================================================

CREATE TABLE IF NOT EXISTS scorecard_workflow_mapping_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_id BIGINT NOT NULL,
    scorecard_id BIGINT NOT NULL,
    previous_stage_id BIGINT DEFAULT NULL,
    new_stage_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    initialize_reporting_date_stages BIT(1) NOT NULL DEFAULT b'0',
    reporting_date_stages_initialized INT NOT NULL DEFAULT 0,
    actor_id BIGINT NOT NULL,
    date_created DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_workflow_mapping_audit_client (client_id),
    INDEX idx_workflow_mapping_audit_scorecard (scorecard_id),
    INDEX idx_workflow_mapping_audit_actor (actor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TEMPORARY TABLE IF EXISTS pm_latest_reporting_date;
CREATE TEMPORARY TABLE pm_latest_reporting_date AS
SELECT
    rp.id AS reporting_period_id,
    (
        SELECT rd.id
        FROM reporting_date rd
        WHERE rd.reporting_period_id = rp.id
        ORDER BY rd.end_date DESC, rd.id DESC
        LIMIT 1
    ) AS reporting_date_id
FROM reporting_period rp;

-- Preserve legacy overall comments where no dated comment exists yet.
INSERT INTO overall_comment (
    scorecard_id,
    reporting_date_id,
    owner_comment,
    supervisor_comment,
    moderator_comment,
    `date`
)
SELECT
    s.id,
    latest.reporting_date_id,
    s.owner_comment,
    s.supervisor_comment,
    s.moderator_comment,
    CURRENT_TIMESTAMP
FROM scorecard s
JOIN pm_latest_reporting_date latest
  ON latest.reporting_period_id = s.reporting_period_id
LEFT JOIN overall_comment existing
  ON existing.scorecard_id = s.id
 AND existing.reporting_date_id = latest.reporting_date_id
WHERE latest.reporting_date_id IS NOT NULL
  AND existing.id IS NULL
  AND (
       NULLIF(TRIM(s.owner_comment), '') IS NOT NULL
    OR NULLIF(TRIM(s.supervisor_comment), '') IS NOT NULL
    OR NULLIF(TRIM(s.moderator_comment), '') IS NOT NULL
  );

-- Preserve legacy aggregate scores where no dated aggregate exists yet.
INSERT INTO overall_score (
    scorecard_id,
    reporting_date_id,
    employee_overall,
    manager_overall,
    agreed_overall,
    moderated_overall,
    `date`
)
SELECT
    s.id,
    latest.reporting_date_id,
    s.employee_score,
    s.manager_score,
    s.agreed_score,
    s.moderated_score,
    CURRENT_TIMESTAMP
FROM scorecard s
JOIN pm_latest_reporting_date latest
  ON latest.reporting_period_id = s.reporting_period_id
LEFT JOIN overall_score existing
  ON existing.scorecard_id = s.id
 AND existing.reporting_date_id = latest.reporting_date_id
WHERE latest.reporting_date_id IS NOT NULL
  AND existing.id IS NULL
  AND (
       COALESCE(s.employee_score, 0) <> 0
    OR COALESCE(s.manager_score, 0) <> 0
    OR COALESCE(s.agreed_score, 0) <> 0
    OR COALESCE(s.moderated_score, 0) <> 0
  );

DROP TEMPORARY TABLE IF EXISTS pm_latest_reporting_date;

-- Orphan aggregates/comments cannot produce meaningful dated results.
DELETE FROM overall_comment
WHERE scorecard_id IS NULL OR reporting_date_id IS NULL;

DELETE FROM overall_score
WHERE scorecard_id IS NULL OR reporting_date_id IS NULL;

-- Merge and remove duplicate dated comments before enforcing uniqueness.
DROP TEMPORARY TABLE IF EXISTS pm_overall_comment_merge;
CREATE TEMPORARY TABLE pm_overall_comment_merge AS
SELECT
    scorecard_id,
    reporting_date_id,
    MAX(id) AS keep_id,
    MAX(NULLIF(owner_comment, '')) AS owner_comment,
    MAX(NULLIF(supervisor_comment, '')) AS supervisor_comment,
    MAX(NULLIF(moderator_comment, '')) AS moderator_comment
FROM overall_comment
GROUP BY scorecard_id, reporting_date_id;

UPDATE overall_comment oc
JOIN pm_overall_comment_merge merged
  ON merged.keep_id = oc.id
SET oc.owner_comment = merged.owner_comment,
    oc.supervisor_comment = merged.supervisor_comment,
    oc.moderator_comment = merged.moderator_comment;

DELETE oc
FROM overall_comment oc
JOIN pm_overall_comment_merge merged
  ON merged.scorecard_id = oc.scorecard_id
 AND merged.reporting_date_id = oc.reporting_date_id
WHERE oc.id <> merged.keep_id;

DROP TEMPORARY TABLE IF EXISTS pm_overall_comment_merge;

ALTER TABLE overall_comment
    MODIFY scorecard_id BIGINT NOT NULL,
    MODIFY reporting_date_id BIGINT NOT NULL,
    MODIFY owner_comment VARCHAR(1000) DEFAULT NULL,
    MODIFY supervisor_comment VARCHAR(1000) DEFAULT NULL,
    MODIFY moderator_comment VARCHAR(1000) DEFAULT NULL;

SET @overall_comment_unique_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'overall_comment'
      AND index_name = 'uk_overall_comment_scorecard_reporting_date'
);
SET @sql = IF(
    @overall_comment_unique_exists = 0,
    'ALTER TABLE overall_comment ADD CONSTRAINT uk_overall_comment_scorecard_reporting_date UNIQUE (scorecard_id, reporting_date_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Keep one authoritative overall score per scorecard/reporting date.
DROP TEMPORARY TABLE IF EXISTS pm_overall_score_keep;
CREATE TEMPORARY TABLE pm_overall_score_keep AS
SELECT scorecard_id, reporting_date_id, MAX(id) AS keep_id
FROM overall_score
GROUP BY scorecard_id, reporting_date_id;

DELETE os
FROM overall_score os
JOIN pm_overall_score_keep keep_row
  ON keep_row.scorecard_id = os.scorecard_id
 AND keep_row.reporting_date_id = os.reporting_date_id
WHERE os.id <> keep_row.keep_id;

DROP TEMPORARY TABLE IF EXISTS pm_overall_score_keep;

ALTER TABLE overall_score
    MODIFY scorecard_id BIGINT NOT NULL,
    MODIFY reporting_date_id BIGINT NOT NULL;

SET @overall_score_unique_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'overall_score'
      AND index_name = 'uk_overall_score_scorecard_reporting_date'
);
SET @sql = IF(
    @overall_score_unique_exists = 0,
    'ALTER TABLE overall_score ADD CONSTRAINT uk_overall_score_scorecard_reporting_date UNIQUE (scorecard_id, reporting_date_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Map any scorecards left unresolved when the workflow-stage table was
-- initially empty. Known legacy statuses are mapped by role first because
-- multiple configured stages may intentionally share one status code.
UPDATE scorecard s
JOIN scorecard_workflow_stage st
  ON st.client_id = s.client_id
 AND UPPER(COALESCE(st.status, 'ACTIVE')) = 'ACTIVE'
 AND UPPER(COALESCE(st.role_key, '')) = (
     CASE UPPER(COALESCE(s.approval_status, ''))
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
SET s.approval_stage_id = st.id
WHERE s.approval_stage_id IS NULL;

-- Custom legacy status codes can still map directly where the match is unique.
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
    WHERE s2.approval_stage_id IS NULL
    GROUP BY s2.id
    HAVING COUNT(DISTINCT st.id) = 1
) mapped ON mapped.scorecard_id = s.id
SET s.approval_stage_id = mapped.stage_id
WHERE s.approval_stage_id IS NULL;

-- Do not discard an approval status that was not mapped by the alignment migration.
DROP PROCEDURE IF EXISTS assert_scorecard_approval_status_mapped;
DELIMITER //
CREATE PROCEDURE assert_scorecard_approval_status_mapped()
BEGIN
    DECLARE unmapped_count BIGINT DEFAULT 0;

    SELECT COUNT(*)
    INTO unmapped_count
    FROM scorecard
    WHERE NULLIF(TRIM(approval_status), '') IS NOT NULL
      AND approval_stage_id IS NULL;

    IF unmapped_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot remove scorecard.approval_status: unresolved approval stages remain';
    END IF;
END//
DELIMITER ;

CALL assert_scorecard_approval_status_mapped();
DROP PROCEDURE IF EXISTS assert_scorecard_approval_status_mapped;

DROP PROCEDURE IF EXISTS drop_pm_scorecard_column_if_exists;
DELIMITER //
CREATE PROCEDURE drop_pm_scorecard_column_if_exists(IN column_name_param VARCHAR(64))
BEGIN
    SET @column_exists = (
        SELECT COUNT(*)
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'scorecard'
          AND column_name = column_name_param
    );

    IF @column_exists > 0 THEN
        SET @drop_column_sql = CONCAT(
            'ALTER TABLE scorecard DROP COLUMN `', column_name_param, '`'
        );
        PREPARE stmt FROM @drop_column_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL drop_pm_scorecard_column_if_exists('employee_score');
CALL drop_pm_scorecard_column_if_exists('manager_score');
CALL drop_pm_scorecard_column_if_exists('agreed_score');
CALL drop_pm_scorecard_column_if_exists('moderated_score');
CALL drop_pm_scorecard_column_if_exists('weighted_score');
CALL drop_pm_scorecard_column_if_exists('owner_comment');
CALL drop_pm_scorecard_column_if_exists('supervisor_comment');
CALL drop_pm_scorecard_column_if_exists('moderator_comment');
CALL drop_pm_scorecard_column_if_exists('approval_status');

DROP PROCEDURE IF EXISTS drop_pm_scorecard_column_if_exists;
