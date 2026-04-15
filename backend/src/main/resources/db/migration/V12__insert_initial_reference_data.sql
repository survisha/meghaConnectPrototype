-- V12__insert_initial_reference_data.sql
-- Insert initial reference data for schemes, categories, and agendas

INSERT IGNORE INTO reference_type (code, name, description, status) VALUES
('CM_SCHEME', 'CM Schemes', 'Chief Minister Schemes', 'ACTIVE'),
('CITIZEN_DESIGNATION', 'Citizen Designations', 'Designations of Citizens', 'ACTIVE'),
('CM_AGENDA_MEETING', 'Meeting Agendas', 'CM Agenda Meeting Types', 'ACTIVE');

-- Insert CM Schemes
INSERT IGNORE INTO reference_data (type_id, code, value, display_order, is_active) 
VALUES 
((SELECT id FROM reference_type WHERE code = 'CM_SCHEME'), 'CMSDF', 'CMSDF – CM Special Development Fund', 1, TRUE),
((SELECT id FROM reference_type WHERE code = 'CM_SCHEME'), 'CMSG', 'CMSG – CM Special Grant', 2, TRUE),
((SELECT id FROM reference_type WHERE code = 'CM_SCHEME'), 'CM_CARE', 'CM Care – Medical Assistance', 3, TRUE),
((SELECT id FROM reference_type WHERE code = 'CM_SCHEME'), 'CM_CONNECT', 'CM Connect – Connectivity', 4, TRUE),
((SELECT id FROM reference_type WHERE code = 'CM_SCHEME'), 'CM_ELEVATE', 'CM Elevate – Youth Employment', 5, TRUE);
((SELECT id FROM reference_type WHERE code = 'CM_SCHEME'), 'FOCUS_PLUS', 'Focus+ – Focused Development', 6, TRUE);
((SELECT id FROM reference_type WHERE code = 'CM_SCHEME'), 'OTHERS', 'OTHERS', 7, TRUE);

-- Insert Project Categories
INSERT IGNORE INTO reference_data (type_id, code, value, display_order, is_active)
VALUES
((SELECT id FROM reference_type WHERE code = 'CITIZEN_DESIGNATION'), 'GOVT_SERVANT', 'Govt Servant', 1, TRUE),
((SELECT id FROM reference_type WHERE code = 'CITIZEN_DESIGNATION'), 'RETIRED_GOVT_SERVANT', 'Retired Govt Servant', 2, TRUE),
((SELECT id FROM reference_type WHERE code = 'CITIZEN_DESIGNATION'), 'TEACHER', 'Teacher', 3, TRUE),
((SELECT id FROM reference_type WHERE code = 'CITIZEN_DESIGNATION'), 'POLITICAL_LEADER', 'Political Leader', 4, TRUE),
((SELECT id FROM reference_type WHERE code = 'CITIZEN_DESIGNATION'), 'STUDENTS', 'Students', 5, TRUE),
((SELECT id FROM reference_type WHERE code = 'CITIZEN_DESIGNATION'), 'RELIGIOUS_LEADER', 'Religious Leader', 6, TRUE),
((SELECT id FROM reference_type WHERE code = 'CITIZEN_DESIGNATION'), 'BUSINESSMAN', 'Businessman', 7, TRUE),
((SELECT id FROM reference_type WHERE code = 'CITIZEN_DESIGNATION'), 'MEDIA', 'Media', 8, TRUE),
((SELECT id FROM reference_type WHERE code = 'CITIZEN_DESIGNATION'), 'GENERAL_PUBLIC', 'General Public', 9, TRUE),
((SELECT id FROM reference_type WHERE code = 'CITIZEN_DESIGNATION'), 'ORGANIZATION', 'Organization (Village Authority, NGO, Institute, etc.)', 10, TRUE);

-- Insert Meeting Agendas
INSERT IGNORE INTO reference_data (type_id, code, value, display_order, is_active)
VALUES
((SELECT id FROM reference_type WHERE code = 'CM_AGENDA_MEETING'), 'SCHEME_AVAILMENT', 'Scheme Availment (CM)', 1, TRUE),
((SELECT id FROM reference_type WHERE code = 'CM_AGENDA_MEETING'), 'GOVERNANCE', 'Governance', 2, TRUE),
((SELECT id FROM reference_type WHERE code = 'CM_AGENDA_MEETING'), 'TRADE_COMMERCE', 'Trade & Commerce', 3, TRUE),
((SELECT id FROM reference_type WHERE code = 'CM_AGENDA_MEETING'), 'POLITICAL_DISCUSSION', 'Political Discussion', 4, TRUE),
((SELECT id FROM reference_type WHERE code = 'CM_AGENDA_MEETING'), 'PUBLIC_GRIEVANCE', 'Public Grievance', 5, TRUE);