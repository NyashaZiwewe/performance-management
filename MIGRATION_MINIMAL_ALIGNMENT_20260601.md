# Minimal Migration Plan (2026-06-01)

Use this when upgrading an existing `performance_management` database with minimal risk.

## Scope

1. Align existing tables that current code now depends on:
   - `reporting_period.model`
   - `scorecard.approval_stage_id` linked to `scorecard_workflow_stage`
   - backfill from existing `scorecard.approval_status` (legacy data retained)
   - `scorecard_reporting_date_stage` to isolate score workflow by reporting date
   - `gear.reporting_period_id`
   - `goal.scorecard_id`, `goal.perspective_id`, `goal.strategic_objective_id`
   - `target.goal_id` backfill only (no forced FK add)
2. Create required strategic hierarchy tables:
   - `perspective`, `strategic_objective`
3. Create probation tables:
   - `probation_assessment`, `probation_kpi`, `probation_assessment_dimension`, etc.

## Run steps

1. Back up the target database.
2. Run:

```bash
mysql -u <user> -p <database> < MIGRATION_MINIMAL_ALIGNMENT_20260601.sql
```

3. Verify:

```sql
SELECT id, start_date, end_date, model FROM reporting_period;
SHOW COLUMNS FROM gear LIKE 'reporting_period_id';
SHOW COLUMNS FROM scorecard LIKE 'approval_stage_id';
SHOW TABLES LIKE 'scorecard_workflow_stage';
SHOW TABLES LIKE 'scorecard_reporting_date_stage';
SHOW COLUMNS FROM goal LIKE 'scorecard_id';
SHOW COLUMNS FROM target LIKE 'goal_id';
SHOW TABLES LIKE 'probation_%';
```

## Notes

- The script is additive and avoids destructive changes.
- No columns are dropped.
- `scorecard.approval_status` is intentionally retained as a legacy column for rollback safety.
- Existing goals remain compatible by defaulting null `scorecard_id` to `0`.
- Potentially inconsistent `target` rows are backed up into `target_fk_repair_backup` for manual review.
- This migration intentionally avoids forcing `target.goal_id` / `target.output_id` FK constraints to prevent startup failures on legacy data.
