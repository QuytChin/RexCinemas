-- Read-only preview. Run this before mysql-rex-rewards-opening-balance-apply.sql.
SELECT id, loyalty_points, lifetime_points, tier_qualifying_points, redeemed_points, reversed_points
FROM app_users
ORDER BY id;

SELECT COUNT(*) AS users_without_opening_entry
FROM app_users u
WHERE NOT EXISTS (
    SELECT 1 FROM loyalty_ledger l
    WHERE l.user_id = u.id AND l.reference_key = CONCAT('MIGRATION-OPENING-', u.id)
);
