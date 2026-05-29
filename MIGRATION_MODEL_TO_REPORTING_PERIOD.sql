-- Migration Script: Move hierarchy model from scorecard to reporting_period
-- Date: 2025-05-29
-- Description: Migrates the 'model' field from scorecard table to reporting_period table
--              All scorecards within a reporting period will inherit the period's model

-- Step 1: Add model column to reporting_period table (if not exists)
ALTER TABLE reporting_period ADD COLUMN IF NOT EXISTS model VARCHAR(50);

-- Step 2: Migrate existing model values from scorecard to reporting_period
-- This sets the reporting_period.model based on the first scorecard's model for each period
UPDATE reporting_period rp
SET model = (
    SELECT s.model
    FROM scorecard s
    WHERE s.reporting_period_id = rp.id
      AND s.model IS NOT NULL
    LIMIT 1
)
WHERE rp.model IS NULL;

-- Step 3: Set default model to 'standard' for reporting periods that still have NULL
UPDATE reporting_period
SET model = 'standard'
WHERE model IS NULL OR model = '';

-- Step 4: Drop model column from scorecard table
-- NOTE: Uncomment the line below ONLY after verifying the migration was successful
-- ALTER TABLE scorecard DROP COLUMN IF EXISTS model;

-- Verification Queries (run these to check the migration):
-- SELECT id, start_date, end_date, model FROM reporting_period;
-- SELECT COUNT(*), model FROM reporting_period GROUP BY model;
-- SELECT id, reporting_period_id, model FROM scorecard WHERE model IS NOT NULL LIMIT 10;
