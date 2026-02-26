-- ============================================================
-- MeghaConnect Seed Data (Demo)
-- ============================================================
-- Passwords are BCrypt hashes of demo passwords

-- USERS
INSERT INTO users (username, password_hash, full_name, role, active, offline_access, created_at)
VALUES
  ('hcm',    '$2a$12$dHwFV1lqTmXpWtQUvkfGfOMGk7oIPfvMT0X5H/EDM4vS.5bIAhzXG', 'Hon. Chief Minister',    'HCM',                  true, true,  NOW()),
  ('admin',  '$2a$12$q1B3Z4Y9pKjH6cz7rR7hq.lG5.Ncp8d8C0g6K6lz7a7J3X4/BWEO.', 'System Admin',           'ADMIN',                true, false, NOW()),
  ('saidul', '$2a$12$pUz3mhWM2n6pQ3KyN0W7fuQfqgN7VRyL/.RKumRgKQ5h1oqFd6TuC', 'Saidul OSD',             'SAIDUL_OSD',           true, false, NOW()),
  ('jtsecy', '$2a$12$Q7f4YcV9XJhBUiD0L5N3RuHm.4Oi9jKnY2vq6TT8sn6wTU7m0aMua', 'Joint Secretary',        'APPROVER_JT_SECY',     true, false, NOW()),
  ('cmo',    '$2a$12$aQ5HYKb7C3jvTqZ6G0W9WuHaM5jb9Y6X9bViKEy9lUXm1CK.nRzFa', 'CMO Officer',            'CMO_OFFICER',          true, false, NOW()),
  ('deo1',   '$2a$12$0Y6h3m7W2K4v9P5H6Rx/L.jBe3N7GW8cQ.vbWn8M3b0aXK7Y5k/Ji', 'Data Entry Operator 1',  'DATA_ENTRY_OPERATOR',  true, false, NOW());

-- SAMPLE PERSONS
INSERT INTO persons (full_name, phone_number, epic_number, designation, district, constituency, booth, village, brief_profile, created_at)
VALUES
  ('Ramsing Marak',  '9876543210', 'MH/01/001/234567', 'Political Leader', 'West Garo Hills',  'Ampati',       'Booth 12', 'Dalu',        'District-level NPP leader.', NOW()),
  ('Sunita Sangma',  '9876500001', 'MH/01/002/345678', 'Teacher',          'East Khasi Hills', 'Shillong East','Booth 5',  'Laitumkhrah', 'Government school teacher.', NOW()),
  ('Bijoy Momin',    '9812345678', 'MH/02/003/456789', 'General Public',   'South Garo Hills', 'Baghmara',     'Booth 3',  'Baghmara Town','Farmer.',                   NOW()),
  ('Deibok Lyngdoh', '9887654321', 'MH/01/004/567890', 'Businessman',      'Ri Bhoi',          'Umsning',      'Booth 7',  'Nongpoh',     'Transport entrepreneur.',    NOW());
