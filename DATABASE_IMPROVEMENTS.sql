-- =====================================================
-- DATABASE IMPROVEMENTS SCRIPT
-- Date: 2025-05-29
-- Description: Adds constraints, indexes, and audit tables
-- =====================================================

-- =====================================================
-- SECTION 1: ADD NOT NULL CONSTRAINTS
-- =====================================================

-- Scorecard table
ALTER TABLE scorecard
    MODIFY COLUMN owner_id BIGINT NOT NULL,
    MODIFY COLUMN reporting_period_id BIGINT NOT NULL,
    MODIFY COLUMN scorecard_model_id BIGINT NOT NULL,
    MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    MODIFY COLUMN approval_status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    MODIFY COLUMN lock_status VARCHAR(50) NOT NULL DEFAULT 'OPEN';

-- ReportingPeriod table
ALTER TABLE reporting_period
    MODIFY COLUMN start_date VARCHAR(255) NOT NULL,
    MODIFY COLUMN end_date VARCHAR(255) NOT NULL,
    MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    MODIFY COLUMN model VARCHAR(50) NOT NULL DEFAULT 'standard';

-- Account table
ALTER TABLE account
    MODIFY COLUMN email VARCHAR(255) NOT NULL,
    MODIFY COLUMN full_name VARCHAR(255) NOT NULL,
    MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- Target table
ALTER TABLE target
    MODIFY COLUMN scorecard_id BIGINT NOT NULL,
    MODIFY COLUMN measure TEXT NOT NULL,
    MODIFY COLUMN unit VARCHAR(100) NOT NULL,
    MODIFY COLUMN allocated_weight DOUBLE NOT NULL DEFAULT 0;

-- =====================================================
-- SECTION 2: ADD CHECK CONSTRAINTS
-- =====================================================

-- Scorecard score constraints
ALTER TABLE scorecard
    ADD CONSTRAINT chk_scorecard_employee_score
        CHECK (employee_score >= 0 AND employee_score <= 5),
    ADD CONSTRAINT chk_scorecard_manager_score
        CHECK (manager_score >= 0 AND manager_score <= 5),
    ADD CONSTRAINT chk_scorecard_agreed_score
        CHECK (agreed_score >= 0 AND agreed_score <= 5),
    ADD CONSTRAINT chk_scorecard_moderated_score
        CHECK (moderated_score >= 0 AND moderated_score <= 5),
    ADD CONSTRAINT chk_scorecard_weighted_score
        CHECK (weighted_score >= 0 AND weighted_score <= 100);

-- Scorecard status constraints
ALTER TABLE scorecard
    ADD CONSTRAINT chk_scorecard_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED', 'DELETED')),
    ADD CONSTRAINT chk_scorecard_approval_status
        CHECK (approval_status IN ('NEW', 'PENDING', 'APPROVED', 'REJECTED', 'RETURNED')),
    ADD CONSTRAINT chk_scorecard_lock_status
        CHECK (lock_status IN ('OPEN', 'LOCKED', 'CLOSED'));

-- ReportingPeriod constraints
ALTER TABLE reporting_period
    ADD CONSTRAINT chk_reporting_period_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    ADD CONSTRAINT chk_reporting_period_model
        CHECK (model IN ('gear', 'programme', 'standard'));

-- Account constraints
ALTER TABLE account
    ADD CONSTRAINT chk_account_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'));

-- Target weight constraints
ALTER TABLE target
    ADD CONSTRAINT chk_target_allocated_weight
        CHECK (allocated_weight >= 0 AND allocated_weight <= 100);

-- Score constraints
ALTER TABLE score
    ADD CONSTRAINT chk_score_employee_score
        CHECK (employee_score IS NULL OR (employee_score >= 0 AND employee_score <= 5)),
    ADD CONSTRAINT chk_score_manager_score
        CHECK (manager_score IS NULL OR (manager_score >= 0 AND manager_score <= 5)),
    ADD CONSTRAINT chk_score_agreed_score
        CHECK (agreed_score IS NULL OR (agreed_score >= 0 AND agreed_score <= 5)),
    ADD CONSTRAINT chk_score_moderated_score
        CHECK (moderated_score IS NULL OR (moderated_score >= 0 AND moderated_score <= 5)),
    ADD CONSTRAINT chk_score_weighted_score
        CHECK (weighted_score IS NULL OR (weighted_score >= 0 AND weighted_score <= 100));

-- =====================================================
-- SECTION 3: ADD UNIQUE CONSTRAINTS
-- =====================================================

-- Email must be unique
ALTER TABLE account
    ADD CONSTRAINT uk_account_email UNIQUE (email);

-- Reporting period dates should be unique per client
ALTER TABLE reporting_period
    ADD CONSTRAINT uk_reporting_period_dates
        UNIQUE (start_date, end_date, client_id);

-- Only one active scorecard per owner per reporting period
CREATE UNIQUE INDEX uk_scorecard_owner_period
    ON scorecard(owner_id, reporting_period_id, status)
    WHERE status = 'ACTIVE';

-- =====================================================
-- SECTION 4: ADD INDEXES FOR PERFORMANCE
-- =====================================================

-- Scorecard indexes
CREATE INDEX idx_scorecard_owner_id ON scorecard(owner_id);
CREATE INDEX idx_scorecard_reporting_period_id ON scorecard(reporting_period_id);
CREATE INDEX idx_scorecard_status ON scorecard(status);
CREATE INDEX idx_scorecard_approval_status ON scorecard(approval_status);
CREATE INDEX idx_scorecard_lock_status ON scorecard(lock_status);
CREATE INDEX idx_scorecard_client_id ON scorecard(client_id);

-- Composite indexes for common queries
CREATE INDEX idx_scorecard_period_status ON scorecard(reporting_period_id, status);
CREATE INDEX idx_scorecard_owner_period ON scorecard(owner_id, reporting_period_id);
CREATE INDEX idx_scorecard_client_status ON scorecard(client_id, status);

-- Target indexes
CREATE INDEX idx_target_scorecard_id ON target(scorecard_id);
CREATE INDEX idx_target_goal_id ON target(goal_id);
CREATE INDEX idx_target_perspective_id ON target(perspective_id);
CREATE INDEX idx_target_strategic_objective_id ON target(strategic_objective_id);

-- Score indexes
CREATE INDEX idx_score_target_id ON score(target_id);
CREATE INDEX idx_score_reporting_date_id ON score(reporting_date_id);
CREATE INDEX idx_score_target_date ON score(target_id, reporting_date_id);

-- Account indexes
CREATE INDEX idx_account_email ON account(email);
CREATE INDEX idx_account_client_id ON account(client_id);
CREATE INDEX idx_account_status ON account(status);
CREATE INDEX idx_account_supervisor_id ON account(supervisor_id);

-- ReportingPeriod indexes
CREATE INDEX idx_reporting_period_client_id ON reporting_period(client_id);
CREATE INDEX idx_reporting_period_status ON reporting_period(status);
CREATE INDEX idx_reporting_period_dates ON reporting_period(start_date, end_date);

-- ReportingDate indexes
CREATE INDEX idx_reporting_date_period_id ON reporting_date(reporting_period_id);
CREATE INDEX idx_reporting_date_status ON reporting_date(status);

-- Goal indexes
CREATE INDEX idx_goal_scorecard_id ON goal(scorecard_id);
CREATE INDEX idx_goal_gear_id ON goal(gear_id);

-- =====================================================
-- SECTION 5: CREATE AUDIT TRAIL TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_name VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT')),
    user_id BIGINT,
    user_name VARCHAR(255),
    user_email VARCHAR(255),
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_id BIGINT NOT NULL,
    INDEX idx_audit_entity (entity_name, entity_id),
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_action (action),
    INDEX idx_audit_client (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- SECTION 6: ADD AUDIT COLUMNS TO MAIN TABLES
-- =====================================================

-- Add created_by and updated_by to scorecard
ALTER TABLE scorecard
    ADD COLUMN created_by BIGINT,
    ADD COLUMN created_by_name VARCHAR(255),
    ADD COLUMN last_modified_by BIGINT,
    ADD COLUMN last_modified_by_name VARCHAR(255),
    ADD INDEX idx_scorecard_created_by (created_by),
    ADD INDEX idx_scorecard_modified_by (last_modified_by);

-- Add to reporting_period
ALTER TABLE reporting_period
    ADD COLUMN created_by BIGINT,
    ADD COLUMN created_by_name VARCHAR(255),
    ADD COLUMN last_modified_by BIGINT,
    ADD COLUMN last_modified_by_name VARCHAR(255);

-- Add to target
ALTER TABLE target
    ADD COLUMN created_by BIGINT,
    ADD COLUMN last_modified_by BIGINT;

-- =====================================================
-- SECTION 7: ADD SOFT DELETE SUPPORT
-- =====================================================

-- Add deleted columns
ALTER TABLE scorecard
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at DATETIME,
    ADD COLUMN deleted_by BIGINT,
    ADD INDEX idx_scorecard_deleted (deleted);

ALTER TABLE target
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at DATETIME,
    ADD COLUMN deleted_by BIGINT,
    ADD INDEX idx_target_deleted (deleted);

ALTER TABLE account
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at DATETIME,
    ADD COLUMN deleted_by BIGINT,
    ADD INDEX idx_account_deleted (deleted);

-- =====================================================
-- SECTION 8: ADD PASSWORD SECURITY COLUMNS
-- =====================================================

ALTER TABLE account
    ADD COLUMN password_changed_at DATETIME,
    ADD COLUMN password_expires_at DATETIME,
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME,
    ADD COLUMN last_login_at DATETIME,
    ADD COLUMN last_login_ip VARCHAR(50);

-- =====================================================
-- SECTION 9: FOREIGN KEY CONSTRAINTS (IF NOT EXISTS)
-- =====================================================

-- Add foreign keys with proper cascading
-- Note: Check if these already exist before running

ALTER TABLE scorecard
    ADD CONSTRAINT fk_scorecard_owner
        FOREIGN KEY (owner_id) REFERENCES account(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_scorecard_reporting_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_period(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_scorecard_model
        FOREIGN KEY (scorecard_model_id) REFERENCES scorecard_model(id) ON DELETE RESTRICT;

ALTER TABLE target
    ADD CONSTRAINT fk_target_scorecard
        FOREIGN KEY (scorecard_id) REFERENCES scorecard(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_target_goal
        FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_target_perspective
        FOREIGN KEY (perspective_id) REFERENCES perspective(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_target_strategic_objective
        FOREIGN KEY (strategic_objective_id) REFERENCES strategic_objective(id) ON DELETE SET NULL;

ALTER TABLE score
    ADD CONSTRAINT fk_score_target
        FOREIGN KEY (target_id) REFERENCES target(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_score_reporting_date
        FOREIGN KEY (reporting_date_id) REFERENCES reporting_date(id) ON DELETE CASCADE;

ALTER TABLE goal
    ADD CONSTRAINT fk_goal_scorecard
        FOREIGN KEY (scorecard_id) REFERENCES scorecard(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_goal_gear
        FOREIGN KEY (gear_id) REFERENCES gear(id) ON DELETE SET NULL;

ALTER TABLE reporting_date
    ADD CONSTRAINT fk_reporting_date_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_period(id) ON DELETE CASCADE;

ALTER TABLE strategic_objective
    ADD CONSTRAINT fk_strategic_objective_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_period(id) ON DELETE CASCADE;

-- =====================================================
-- SECTION 10: CREATE NOTIFICATION TRACKING TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS notification_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255),
    subject VARCHAR(500),
    message TEXT,
    notification_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    status VARCHAR(50) NOT NULL CHECK (status IN ('SENT', 'FAILED', 'PENDING')),
    error_message TEXT,
    sent_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_recipient (recipient_email),
    INDEX idx_notification_status (status),
    INDEX idx_notification_entity (entity_type, entity_id),
    INDEX idx_notification_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Run these to verify the changes

-- Check constraints
SELECT
    TABLE_NAME,
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME IN ('scorecard', 'target', 'score', 'account', 'reporting_period')
ORDER BY TABLE_NAME, CONSTRAINT_TYPE;

-- Check indexes
SELECT
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME IN ('scorecard', 'target', 'score', 'account')
ORDER BY TABLE_NAME, INDEX_NAME;

-- Check audit_log table
SELECT COUNT(*) as audit_log_exists FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_log';

-- =====================================================
-- ROLLBACK SCRIPT (Save separately if needed)
-- =====================================================

-- If you need to rollback, uncomment and run these:

-- DROP TABLE IF EXISTS audit_log;
-- DROP TABLE IF EXISTS notification_log;

-- Remove added columns:
-- ALTER TABLE scorecard DROP COLUMN created_by, DROP COLUMN created_by_name,
--     DROP COLUMN last_modified_by, DROP COLUMN last_modified_by_name,
--     DROP COLUMN deleted, DROP COLUMN deleted_at, DROP COLUMN deleted_by;

-- Remove constraints (list them individually)
-- ALTER TABLE scorecard DROP CONSTRAINT chk_scorecard_employee_score;
-- ...etc
