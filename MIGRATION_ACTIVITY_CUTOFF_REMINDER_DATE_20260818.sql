-- Adds a separate marker for cutoff reminders so activity-open notices and
-- cutoff notices do not suppress each other.
-- Target DB: performance_management (MySQL 8+)

ALTER TABLE reporting_date_activity_period
    ADD COLUMN last_cutoff_reminder_date VARCHAR(10) DEFAULT NULL;
