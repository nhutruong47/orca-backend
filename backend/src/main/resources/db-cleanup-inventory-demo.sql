-- ============================================================================
-- ORCA Inventory Cleanup
-- ----------------------------------------------------------------------------
-- Removes the auto-seeded "demo" inventory rows that were created by the
-- legacy InventoryBackfillRunner / MockDataInitializer / initializeDefaultInventory
-- code paths. These rows are recognizable by their product_type / product_state
-- being one of {Arabica, Robusta, Culi, Blend} × {GREEN, ROASTED, GROUND, PACKAGED}
-- AND having quantity = 0 (the seeder never gave them stock).
--
-- The schema is "inventory_items" with at least these columns:
--   product_type, product_state, quantity, created_at (nullable)
-- Adjust the column list if your environment differs.
--
-- This script is idempotent — running it multiple times is safe.
-- ============================================================================

BEGIN;

-- 1. Preview the rows that will be deleted (count by product_type).
SELECT product_type, COUNT(*) AS will_be_deleted
FROM inventory_items
WHERE quantity = 0
  AND product_type IN ('Arabica', 'Robusta', 'Culi', 'Blend')
  AND product_state IN ('GREEN', 'ROASTED', 'GROUND', 'PACKAGED')
GROUP BY product_type
ORDER BY product_type;

-- 2. Delete the demo rows.
DELETE FROM inventory_items
WHERE quantity = 0
  AND product_type IN ('Arabica', 'Robusta', 'Culi', 'Blend')
  AND product_state IN ('GREEN', 'ROASTED', 'GROUND', 'PACKAGED');

-- 3. Show the new total per team (should now be 0 for any newly-created team).
SELECT team_id, COUNT(*) AS remaining_rows
FROM inventory_items
GROUP BY team_id
ORDER BY remaining_rows DESC
LIMIT 20;

COMMIT;

-- ============================================================================
-- Note: This script does NOT touch inventory rows that have quantity > 0,
-- even if they look like they came from a demo factory — those represent
-- real stock that someone manually entered, and should be preserved.
-- ============================================================================