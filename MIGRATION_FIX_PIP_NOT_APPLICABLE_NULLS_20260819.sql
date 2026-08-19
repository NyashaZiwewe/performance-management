-- Repairs deployments where Hibernate ddl-auto created not_applicable as nullable
-- before MIGRATION_EXTEND_PIP_SELF_ASSESSMENT_20260816.sql was applied.
-- Target DB: performance_management (MySQL 8+)

UPDATE performance_improvement_plan
SET not_applicable = b'0'
WHERE not_applicable IS NULL;

ALTER TABLE performance_improvement_plan
    MODIFY COLUMN not_applicable BIT(1) NOT NULL DEFAULT b'0';
