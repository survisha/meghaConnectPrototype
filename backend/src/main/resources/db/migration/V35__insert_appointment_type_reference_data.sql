-- V35__insert_appointment_type_reference_data.sql
-- Seed appointment/event types as reference data for scheduling screens.
-- Note: The reference type code keeps the spelling requested by the UI requirement.

INSERT IGNORE INTO reference_type (code, name, description, status) VALUES
('APPOINMENT_TYPES', 'Appointment Types', 'Appointment event types used for scheduling and filtering', 'ACTIVE');

INSERT IGNORE INTO reference_data (type_id, code, value, display_order, is_active)
VALUES
((SELECT id FROM reference_type WHERE code = 'APPOINMENT_TYPES'), 'A1', 'A1: Cabinet/Flight', 1, TRUE),
((SELECT id FROM reference_type WHERE code = 'APPOINMENT_TYPES'), 'A2', 'A2: Events', 2, TRUE),
((SELECT id FROM reference_type WHERE code = 'APPOINMENT_TYPES'), 'A3', 'A3: File Clearing / Birthday', 3, TRUE),
((SELECT id FROM reference_type WHERE code = 'APPOINMENT_TYPES'), 'A4', 'A4: Individual Appointments', 4, TRUE),
((SELECT id FROM reference_type WHERE code = 'APPOINMENT_TYPES'), 'B1', 'B1: Public Durbar', 5, TRUE),
((SELECT id FROM reference_type WHERE code = 'APPOINMENT_TYPES'), 'B2', 'B2: Walk-in', 6, TRUE);
