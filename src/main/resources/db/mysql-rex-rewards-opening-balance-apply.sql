-- Execute only after a backup and an approved preview. This script is idempotent.
START TRANSACTION;
UPDATE app_users
SET tier_qualifying_points = lifetime_points
WHERE tier_qualifying_points = 0 AND lifetime_points > 0;

INSERT INTO loyalty_ledger (
    user_id, transaction_type, source, reference_key,
    points_delta, available_before, available_after,
    qualifying_delta, qualifying_before, qualifying_after,
    lifetime_delta, lifetime_before, lifetime_after,
    redeemed_delta, reversed_delta, reason, actor
)
SELECT u.id, 'MIGRATION_OPENING', 'MIGRATION', CONCAT('MIGRATION-OPENING-', u.id),
       0, u.loyalty_points, u.loyalty_points,
       0, u.tier_qualifying_points, u.tier_qualifying_points,
       0, u.lifetime_points, u.lifetime_points,
       0, 0, 'Opening balance imported without replaying historic bookings', 'migration'
FROM app_users u
WHERE NOT EXISTS (
    SELECT 1 FROM loyalty_ledger l
    WHERE l.reference_key = CONCAT('MIGRATION-OPENING-', u.id)
);
COMMIT;
