-- Prepared MySQL migration. Review the preflight results and take a backup before running.
-- This file is intentionally not auto-applied by Spring Boot.

-- T01/T02/T03 preflight: every snapshot must point to one booking and booking_code
-- must already be unique before the foreign key is added.
SELECT COUNT(*) AS orphan_snapshot_count
FROM bank_transfer_snapshots s
LEFT JOIN bookings b ON b.booking_code = s.booking_code
WHERE b.id IS NULL;

SELECT booking_code, COUNT(*) AS duplicate_count
FROM bank_transfer_snapshots
GROUP BY booking_code
HAVING COUNT(*) > 1;

-- Stop and investigate if orphan_snapshot_count is non-zero or duplicate_count returns rows.
-- The next statement must be run only after the preflight is clean. It is deliberately
-- explicit so an existing schema cannot silently lose or rewrite historical snapshots.
ALTER TABLE bank_transfer_snapshots
    ADD CONSTRAINT fk_bank_transfer_snapshot_booking
    FOREIGN KEY (booking_code) REFERENCES bookings(booking_code);

-- T04/T12 postflight.
SHOW CREATE TABLE bank_transfer_snapshots;
SHOW INDEX FROM bank_transfer_snapshots;
EXPLAIN SELECT booking_code, bank_name, bank_code, bank_account, account_holder,
                amount, payment_reference, expires_at
         FROM bank_transfer_snapshots
         WHERE booking_code = 'REX_TEST_PREFLIGHT';
