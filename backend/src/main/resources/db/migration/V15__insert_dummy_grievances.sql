-- ============================================================
-- Insert Dummy Grievances for Testing
-- ============================================================
-- This migration adds sample grievances for testing the grievance management system

INSERT INTO grievances (ticket_id, applicant_name, phone_number, district, constituency, category, subject, description, status, submitted_at, created_at)
VALUES
  ('GRV/2026/001', 'Rajesh Kumar', '9876543210', 'East Khasi Hills', 'Shillong', 'INFRASTRUCTURE', 'Pothole in Main Street', 'There is a large pothole on Main Street near the market that is causing traffic issues and vehicle damage.', 'SUBMITTED', NOW(), NOW()),
  ('GRV/2026/002', 'Sneha Sharma', '9876543211', 'West Khasi Hills', 'Nongstoin', 'PUBLIC_SERVICES', 'Delayed Passport Processing', 'Applied for passport 3 months ago but haven''t received any update despite multiple follow-ups.', 'ACKNOWLEDGED', NOW(), NOW()),
  ('GRV/2026/003', 'Vikram Singh', '9876543212', 'Ri Bhoi', 'Nongpoh', 'HEALTH', 'Poor Hygiene in Health Center', 'The primary health center in our village lacks basic sanitation and cleanliness standards.', 'UNDER_REVIEW', NOW(), NOW()),
  ('GRV/2026/004', 'Priya Devi', '9876543213', 'East Jaintia Hills', 'Jowai', 'EDUCATION', 'Lack of Teaching Staff', 'Our school is severely understaffed with only 2 teachers for 150 students across all classes.', 'FORWARDED', NOW(), NOW()),
  ('GRV/2026/005', 'Arun Patel', '9876543214', 'East Garo Hills', 'Tura', 'EMPLOYMENT', 'Unpaid Salary for 2 Months', 'I have not received my salary for the last two months from my employer.', 'RESOLVED', NOW(), NOW()),
  ('GRV/2026/006', 'Meena Gogoi', '9876543215', 'West Garo Hills', 'Baghmara', 'WELFARE_SCHEME', 'No Assistance for Flood Affected Family', 'Our family was affected by recent floods but haven''t received any relief from the government scheme.', 'SUBMITTED', NOW(), NOW()),
  ('GRV/2026/007', 'Karan Bhat', '9876543216', 'East Khasi Hills', 'Shillong', 'LAW_ORDER', 'Harassment by Local Authorities', 'Being harassed repeatedly without valid cause by local enforcement officers.', 'ACKNOWLEDGED', NOW(), NOW()),
  ('GRV/2026/008', 'Anita Roy', '9876543217', 'South Garo Hills', 'Baghmara', 'OTHERS', 'General Complaint on Municipal Services', 'Garbage collection is not happening on schedule in our locality.', 'SUBMITTED', NOW(), NOW()),
  ('GRV/2026/009', 'Deepak Kumar', '9876543218', 'East Khasi Hills', 'Dawki', 'INFRASTRUCTURE', 'Water Supply Issues', 'Water supply has been cut off for 10 days with no explanation from authorities.', 'UNDER_REVIEW', NOW(), NOW()),
  ('GRV/2026/010', 'Neha Singh', '9876543219', 'West Khasi Hills', 'Mawkyrwat', 'HEALTH', 'Unavailability of Essential Medicines', 'The district hospital lacks essential medicines needed for treatment of chronic diseases.', 'ACKNOWLEDGED', NOW(), NOW());

-- Add remarks to resolved grievances
UPDATE grievances SET remarks = 'Pothole has been filled and road surface smoothed. Issue resolved.' WHERE ticket_id = 'GRV/2026/005';
UPDATE grievances SET assigned_department = 'Public Works Department' WHERE ticket_id IN ('GRV/2026/001', 'GRV/2026/005', 'GRV/2026/009');
UPDATE grievances SET assigned_department = 'Health Department' WHERE ticket_id IN ('GRV/2026/003', 'GRV/2026/010');
UPDATE grievances SET assigned_department = 'Education Department' WHERE ticket_id = 'GRV/2026/004';
