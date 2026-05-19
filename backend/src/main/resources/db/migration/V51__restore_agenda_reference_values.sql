-- Restore CM agenda values that may have been overwritten by an earlier V50 attempt,
-- then keep Invitation as an additional agenda option.

UPDATE reference_data rd
JOIN reference_type rt ON rt.id = rd.type_id
SET rd.value = 'Scheme Availment (CM)',
    rd.display_order = 1,
    rd.is_active = TRUE
WHERE rt.code = 'CM_AGENDA_MEETING'
  AND rd.code = 'SCHEME_AVAILMENT';

UPDATE reference_data rd
JOIN reference_type rt ON rt.id = rd.type_id
SET rd.value = 'Governance',
    rd.display_order = 2,
    rd.is_active = TRUE
WHERE rt.code = 'CM_AGENDA_MEETING'
  AND rd.code = 'GOVERNANCE';

UPDATE reference_data rd
JOIN reference_type rt ON rt.id = rd.type_id
SET rd.value = 'Trade & Commerce',
    rd.display_order = 3,
    rd.is_active = TRUE
WHERE rt.code = 'CM_AGENDA_MEETING'
  AND rd.code = 'TRADE_COMMERCE';

UPDATE reference_data rd
JOIN reference_type rt ON rt.id = rd.type_id
SET rd.value = 'Political Discussion',
    rd.display_order = 4,
    rd.is_active = TRUE
WHERE rt.code = 'CM_AGENDA_MEETING'
  AND rd.code = 'POLITICAL_DISCUSSION';

UPDATE reference_data rd
JOIN reference_type rt ON rt.id = rd.type_id
SET rd.value = 'Public Grievance',
    rd.display_order = 5,
    rd.is_active = TRUE
WHERE rt.code = 'CM_AGENDA_MEETING'
  AND rd.code = 'PUBLIC_GRIEVANCE';

INSERT INTO reference_data (type_id, code, value, display_order, is_active)
SELECT rt.id, 'INVITATION', 'Invitation', 6, TRUE
FROM reference_type rt
WHERE rt.code = 'CM_AGENDA_MEETING'
  AND NOT EXISTS (
      SELECT 1
      FROM reference_data rd
      WHERE rd.type_id = rt.id
        AND rd.code = 'INVITATION'
  );

UPDATE reference_data rd
JOIN reference_type rt ON rt.id = rd.type_id
SET rd.value = 'Invitation',
    rd.display_order = 6,
    rd.is_active = TRUE
WHERE rt.code = 'CM_AGENDA_MEETING'
  AND rd.code = 'INVITATION';
