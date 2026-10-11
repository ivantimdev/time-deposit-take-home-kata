-- Sample data for manual exploration, loaded only with the local profile.
-- ON CONFLICT keeps rows that already exist when this repeatable script runs again.

INSERT INTO time_deposits (id, plan_type, days, balance) VALUES
    (1, 'basic',   45,  10000.00),
    (2, 'student', 100, 10000.00),
    (3, 'premium', 60,  10000.00),
    (4, 'premium', 20,  10000.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES
    (1, 1, 500.00,  '2026-09-01'),
    (2, 1, 250.00,  '2026-10-01'),
    (3, 3, 1000.00, '2026-09-15')
ON CONFLICT (id) DO NOTHING;

-- The explicit ids above bypass the identity sequences; move them past those ids so that
-- later inserts without an id do not collide.
SELECT setval(pg_get_serial_sequence('time_deposits', 'id'), (SELECT max(id) FROM time_deposits));
SELECT setval(pg_get_serial_sequence('withdrawals', 'id'), (SELECT max(id) FROM withdrawals));
