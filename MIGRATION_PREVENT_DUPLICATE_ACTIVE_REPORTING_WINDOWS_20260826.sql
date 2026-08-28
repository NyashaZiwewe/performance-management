-- Prevent duplicate active reporting windows.
--
-- Before applying this migration, resolve any rows returned by:
--   SELECT client_id, COUNT(*) FROM reporting_period
--   WHERE UPPER(COALESCE(status, '')) = 'ACTIVE'
--   GROUP BY client_id HAVING COUNT(*) > 1;
--
--   SELECT reporting_period_id, COUNT(*) FROM reporting_date
--   WHERE UPPER(COALESCE(status, '')) IN ('OPEN', 'ACTIVE')
--   GROUP BY reporting_period_id HAVING COUNT(*) > 1;

SET @active_period_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reporting_period'
      AND column_name = 'active_reporting_period_client_id'
);

SET @sql = IF(
    @active_period_column_exists = 0,
    'ALTER TABLE reporting_period ADD COLUMN active_reporting_period_client_id BIGINT GENERATED ALWAYS AS (CASE WHEN UPPER(COALESCE(status, '''')) = ''ACTIVE'' THEN client_id ELSE NULL END) STORED',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @active_period_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'reporting_period'
      AND index_name = 'uk_reporting_period_single_active_client'
);

SET @sql = IF(
    @active_period_index_exists = 0,
    'CREATE UNIQUE INDEX uk_reporting_period_single_active_client ON reporting_period(active_reporting_period_client_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @open_date_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reporting_date'
      AND column_name = 'open_reporting_date_period_id'
);

SET @sql = IF(
    @open_date_column_exists = 0,
    'ALTER TABLE reporting_date ADD COLUMN open_reporting_date_period_id BIGINT GENERATED ALWAYS AS (CASE WHEN UPPER(COALESCE(status, '''')) IN (''OPEN'', ''ACTIVE'') THEN reporting_period_id ELSE NULL END) STORED',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @open_date_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'reporting_date'
      AND index_name = 'uk_reporting_date_single_open_period'
);

SET @sql = IF(
    @open_date_index_exists = 0,
    'CREATE UNIQUE INDEX uk_reporting_date_single_open_period ON reporting_date(open_reporting_date_period_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
