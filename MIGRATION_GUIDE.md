# Migration Guide: Hierarchy Model from Scorecard to Reporting Period

## Overview
This migration moves the hierarchy model (`model` field) from the `scorecard` table to the `reporting_period` table. This centralizes the hierarchy configuration at the reporting period level, ensuring all scorecards within a period use the same hierarchy structure.

## Changes Summary

### 1. Database Changes
- **Added:** `model` column to `reporting_period` table
- **Removed:** `model` column from `scorecard` table (after migration)

### 2. Entity Changes
- **ReportingPeriod.java:** Added `private String model;` field
- **Scorecard.java:** Removed `private String model;` field

### 3. Code Changes
- **ScorecardController.addTerminology():** Now reads from `reportingPeriod.model` instead of `scorecard.model`
- **GearService:** Updated all methods to use `reportingPeriod.model` via helper method

## Migration Steps

### Step 1: Run the SQL Migration Script

Execute the SQL script `MIGRATION_MODEL_TO_REPORTING_PERIOD.sql`:

```bash
# For MySQL/MariaDB
mysql -u your_username -p your_database < MIGRATION_MODEL_TO_REPORTING_PERIOD.sql

# For PostgreSQL
psql -U your_username -d your_database -f MIGRATION_MODEL_TO_REPORTING_PERIOD.sql
```

**What the script does:**
1. Adds `model` column to `reporting_period` table
2. Migrates existing model values from the first scorecard in each period
3. Sets default value to 'standard' for any null values
4. **Note:** Column drop from scorecard is commented out - verify migration first!

### Step 2: Verify Migration

Run these verification queries:

```sql
-- Check all reporting periods have a model set
SELECT id, start_date, end_date, model
FROM reporting_period
WHERE model IS NULL OR model = '';

-- Count models by type
SELECT model, COUNT(*) as count
FROM reporting_period
GROUP BY model;

-- Check if any scorecards still have model values (before column drop)
SELECT COUNT(*) as scorecards_with_model
FROM scorecard
WHERE model IS NOT NULL AND model != '';
```

### Step 3: Deploy Code Changes

After verifying the migration:

1. Build the application:
   ```bash
   mvn clean package
   ```

2. Deploy the new version

3. Test the following:
   - Create a new reporting period with a hierarchy model
   - View existing reporting periods (should show model badges)
   - Create a scorecard under the reporting period
   - Verify scorecard displays use the correct hierarchy columns

### Step 4: Drop Scorecard Model Column (Optional)

**Only after thorough testing**, uncomment and run the drop column statement:

```sql
ALTER TABLE scorecard DROP COLUMN IF EXISTS model;
```

## Hierarchy Model Options

When creating/editing a reporting period, admins select from:

| Value | Label | Hierarchy Structure |
|-------|-------|-------------------|
| `gear` | Gear | Outcome → Output |
| `programme` | Programme | Outcome → Pillar → Strategic Goal → Output |
| `standard` | Standard | Perspective → Strategic Objective → Goal |

## UI Changes

### Admin Pages Updated:
1. **Add Reporting Period** - New "Hierarchy Model" dropdown (required field)
2. **Edit Reporting Period** - Shows current model with dropdown to change
3. **View Reporting Periods** - New "Hierarchy Model" column with badges

### Scorecard Pages (No Changes Required):
All scorecard templates automatically adapt based on the reporting period's model:
- Capture Targets
- View Scorecard
- Capture Employee/Manager/Agreed/Moderated Scores (VALUE_BASED)
- Capture Actual Scores (STANDARD_SCORECARD)

## Rollback Plan

If issues occur:

1. Restore the `model` column in scorecard table:
   ```sql
   ALTER TABLE scorecard ADD COLUMN model VARCHAR(50);
   ```

2. Restore model values from reporting_period back to scorecard:
   ```sql
   UPDATE scorecard s
   JOIN reporting_period rp ON s.reporting_period_id = rp.id
   SET s.model = rp.model;
   ```

3. Revert code changes using git:
   ```bash
   git revert <commit-hash>
   ```

## Testing Checklist

- [ ] Migration script runs without errors
- [ ] All reporting periods have a model value
- [ ] Create new reporting period with each model type (gear/programme/standard)
- [ ] Edit existing reporting period and change model
- [ ] View reporting periods list shows model badges correctly
- [ ] Create scorecard under gear model period - verify hierarchy displays
- [ ] Create scorecard under programme model period - verify hierarchy displays
- [ ] Create scorecard under standard model period - verify hierarchy displays
- [ ] Capture targets with each model type
- [ ] Capture scores with each model type
- [ ] View scorecard with each model type
- [ ] Build succeeds without compilation errors
- [ ] No runtime exceptions when accessing scorecards

## Support

If you encounter issues during migration:
1. Check application logs for errors
2. Verify database migration completed successfully
3. Ensure all code files were updated correctly
4. Contact development team with error details
