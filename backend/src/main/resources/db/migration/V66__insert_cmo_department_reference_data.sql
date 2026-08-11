-- Add the Chief Minister's Office omitted from the original department reference data.

INSERT IGNORE INTO reference_data (type_id, code, value, display_order, is_active)
VALUES (
    (SELECT id FROM reference_type WHERE code = 'DEPARTMENT'),
    'CMO',
    'Chief Minister''s Office',
    50,
    TRUE
);
