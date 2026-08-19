-- Adds scorecard self-assessment traceability to performance improvement plans.
-- Target DB: performance_management (MySQL 8+)

ALTER TABLE performance_improvement_plan
    ADD COLUMN scorecard_id BIGINT DEFAULT NULL,
    ADD COLUMN reporting_date_id BIGINT DEFAULT NULL,
    ADD COLUMN target_id BIGINT DEFAULT NULL,
    ADD COLUMN source VARCHAR(255) DEFAULT NULL,
    ADD COLUMN not_applicable BIT(1) NOT NULL DEFAULT b'0';

CREATE INDEX idx_pip_scorecard_reporting_source
    ON performance_improvement_plan (scorecard_id, reporting_date_id, source);

CREATE INDEX idx_pip_target
    ON performance_improvement_plan (target_id);

ALTER TABLE performance_improvement_plan
    ADD CONSTRAINT fk_pip_scorecard
        FOREIGN KEY (scorecard_id) REFERENCES scorecard (id),
    ADD CONSTRAINT fk_pip_reporting_date
        FOREIGN KEY (reporting_date_id) REFERENCES reporting_date (id);
