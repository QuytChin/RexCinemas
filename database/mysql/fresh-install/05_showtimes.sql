-- Rolling schedule: eight calendar days including today, four separated slots per room.
-- End times are derived from movie duration plus a 15-minute room turnover.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `showtimes` (`id`, `movie_id`, `auditorium_id`, `start_time`, `end_time`, `base_price`, `active`)
SELECT 10000 + d.`day_no` * 20 + (a.`id` - 1) * 4 + s.`slot_no` + 1,
       CASE WHEN d.`day_no` = 7 AND a.`id` = 1 AND s.`slot_no` = 0 THEN 5
            ELSE 1 + MOD(d.`day_no` + a.`id` + s.`slot_no` - 1, 4) END,
       a.`id`, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL d.`day_no` DAY), s.`start_clock`),
       ADDTIME(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL d.`day_no` DAY), s.`start_clock`),
               SEC_TO_TIME((m.`duration_minutes` + 15) * 60)),
       CASE WHEN s.`slot_no` < 2 THEN 85000.00 ELSE 95000.00 END,
       1
FROM `auditoriums` a
CROSS JOIN (
  SELECT 0 AS `day_no` UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
  UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) d
CROSS JOIN (
  SELECT 0 AS `slot_no`, '09:30:00' AS `start_clock`
  UNION ALL SELECT 1, '13:30:00'
  UNION ALL SELECT 2, '17:30:00'
  UNION ALL SELECT 3, '21:30:00'
) s
JOIN `movies` m ON m.`id` = CASE WHEN d.`day_no` = 7 AND a.`id` = 1 AND s.`slot_no` = 0 THEN 5
                                ELSE 1 + MOD(d.`day_no` + a.`id` + s.`slot_no` - 1, 4) END;

-- Historic showtimes back the demo booking, check-in, cancelled, expired and refund cases.
INSERT INTO `showtimes` (`id`, `movie_id`, `auditorium_id`, `start_time`, `end_time`, `base_price`, `active`) VALUES
  (1001, 1, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '12:20:00'), 85000.00, 0),
  (1002, 2, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '14:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '16:07:00'), 90000.00, 0),
  (1003, 3, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '18:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '20:13:00'), 85000.00, 0),
  (1004, 4, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '21:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '22:57:00'), 80000.00, 0),
  (1005, 1, 4, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '19:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '21:20:00'), 85000.00, 0);

INSERT INTO `showtime_seats` (`showtime_id`, `seat_id`, `price`, `status`, `version`)
SELECT st.`id`, s.`id`,
       st.`base_price` + CASE s.`type` WHEN 'VIP' THEN 20000.00 WHEN 'COUPLE' THEN 40000.00 ELSE 0.00 END,
       'AVAILABLE', 0
FROM `showtimes` st
JOIN `seats` s ON s.`auditorium_id` = st.`auditorium_id`;
