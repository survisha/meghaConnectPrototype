import { Injectable } from '@angular/core';
import { Appointment, Visitor, SchemeApplication, AuditEntry, Grievance } from '../models';

@Injectable({ providedIn: 'root' })
export class MockDataService {

  visitors: Visitor[] = [
    { id: 1, fullName: 'Ramsing Marak', phoneNumber: '9876543210', epicNumber: 'MH/01/001/234567', photoUrl: '', designation: 'Political Leader', district: 'West Garo Hills', constituency: 'Ampati', booth: 'Booth 12', village: 'Dalu', briefProfile: 'District-level NPP leader with strong grassroots connect.' },
    { id: 2, fullName: 'Sunita Sangma', phoneNumber: '9876500001', epicNumber: 'MH/01/002/345678', photoUrl: '', designation: 'Teacher', district: 'East Khasi Hills', constituency: 'Shillong East', booth: 'Booth 5', village: 'Laitumkhrah', briefProfile: 'Government school teacher, active in community.' },
    { id: 3, fullName: 'Bijoy Momin', phoneNumber: '9812345678', epicNumber: 'MH/02/003/456789', photoUrl: '', designation: 'General Public', district: 'South Garo Hills', constituency: 'Baghmara', booth: 'Booth 3', village: 'Baghmara Town', briefProfile: 'Farmer seeking agricultural support.' },
    { id: 4, fullName: 'Deibok Lyngdoh', phoneNumber: '9887654321', epicNumber: 'MH/01/004/567890', photoUrl: '', designation: 'Businessman', district: 'Ri Bhoi', constituency: 'Umsning', booth: 'Booth 7', village: 'Nongpoh', briefProfile: 'Small entrepreneur in transport sector.' },
  ];

  appointments: Appointment[] = [
    {
      id: 1, applicationId: 'MC-2024-00001',
      applicant: this.visitors[0],
      agendaType: 'Scheme availment (CM)',
      agendaBrief: 'CMSDF application for community hall construction at Dalu village. Benefits ~500 people.',
      status: 'HCM_PENDING',
      requestedLocation: 'TURA',
      scheduledDateTime: '2024-03-15T10:00:00',
      scheduledDurationMinutes: 30,
      eventType: 'A4', mlaMdcApproved: true, meetingCountLast6Months: 2,
      cmoRemarks: 'Verified application. Community contribution 20%. MLA letter attached.',
      directions: [
        { id: 1, appointmentId: 1, color: 'GREEN', directionText: 'Expedite CMSDF approval for community hall', assignedDepartment: 'Planning Dept', deadline: '2024-04-01', currentStatus: 'Under review', isCompleted: false }
      ]
    },
    {
      id: 2, applicationId: 'MC-2024-00002',
      applicant: this.visitors[1],
      agendaType: 'Public Grievance',
      agendaBrief: 'Request for school infrastructure improvement - repair of classrooms and addition of computer lab.',
      status: 'CMO_REVIEW',
      requestedLocation: 'SHILLONG',
      eventType: 'A4', mlaMdcApproved: false, meetingCountLast6Months: 0,
      cmoRemarks: 'Documents verified. Awaiting approval.',
      shortNotes: 'Teacher seeking infra support for Govt school. No prior scheme receipts. MLA endorsement pending.',
    },
    {
      id: 3, applicationId: 'MC-2024-00003',
      applicant: this.visitors[2],
      agendaType: 'Scheme availment (CM)',
      agendaBrief: 'CM Care application for medical treatment - cardiac surgery required.',
      status: 'SCHEDULED',
      requestedLocation: 'TURA',
      scheduledDateTime: '2024-03-16T14:00:00',
      scheduledDurationMinutes: 20,
      eventType: 'B1', mlaMdcApproved: false, meetingCountLast6Months: 1,
    },
    {
      id: 4, applicationId: 'MC-2024-00004',
      applicant: this.visitors[3],
      agendaType: 'Trade & Commerce',
      agendaBrief: 'Request for transport permit for new pickup van under CM Elevate scheme.',
      status: 'APPROVER_REVIEW',
      requestedLocation: 'SHILLONG',
      eventType: 'A4', mlaMdcApproved: false, meetingCountLast6Months: 0,
      isWalkIn: true,
      cmoRemarks: 'Walk-in visitor. Transport permit request – CM Elevate eligible.',
      shortNotes: 'Young entrepreneur requesting transport permit under CM Elevate. No prior receipts. Walk-in. Docs submitted.',
    },
  ];

  schemeApplications: SchemeApplication[] = [
    { id: 1, applicant: this.visitors[0], schemeType: 'CMSDF', projectName: 'Community Hall, Dalu', projectCategory: 'Community hall', beneficiaryType: 'Community/Society', beneficiaryCount: '501 TO 1000', estimatedCost: 2500000, communityContribution: 500000, justification: 'No community hall in the village. Required for community events.', status: 'HCM_PENDING' },
    { id: 2, applicant: this.visitors[2], schemeType: 'CM_CARE', projectName: 'Cardiac Surgery - Bijoy Momin', projectCategory: 'Medical', beneficiaryType: 'Individual', beneficiaryCount: '1 TO 100', estimatedCost: 350000, communityContribution: 0, justification: 'Patient requires urgent cardiac bypass surgery. BPL family.', status: 'SCHEDULED', hcmDecision: 'APPROVED', hcmApprovedCost: 300000 },
    { id: 3, applicant: this.visitors[3], schemeType: 'CM_ELEVATE', projectName: 'Pickup Van for transport business', projectCategory: 'Buses', beneficiaryType: 'Individual', beneficiaryCount: '1 TO 100', estimatedCost: 800000, communityContribution: 200000, justification: 'Self-employment opportunity for youth entrepreneur.', status: 'SUBMITTED' },
  ];

  auditLogs: AuditEntry[] = [
    { id: 1, entityType: 'Appointment', entityId: 1, action: 'STATUS_CHANGE', details: 'Status changed from SUBMITTED to CMO_REVIEW', performedBy: 'cmo.officer1', timestamp: '2024-03-14T09:15:00' },
    { id: 2, entityType: 'Appointment', entityId: 1, action: 'STATUS_CHANGE', details: 'Status changed from CMO_REVIEW to APPROVER_REVIEW', performedBy: 'cmo.officer1', timestamp: '2024-03-14T11:30:00' },
    { id: 3, entityType: 'Appointment', entityId: 1, action: 'STATUS_CHANGE', details: 'Approver approved – forwarded to HCM', performedBy: 'jt.secy1', timestamp: '2024-03-14T15:00:00' },
    { id: 4, entityType: 'SchemeApplication', entityId: 2, action: 'HCM_APPROVED', details: 'HCM approved CM Care application. Amount: ₹3,00,000', performedBy: 'hcm', timestamp: '2024-03-15T10:45:00' },
    { id: 5, entityType: 'User', entityId: 99, action: 'LOGIN', details: 'User logged in from 192.168.1.10', performedBy: 'cmo.officer1', timestamp: '2024-03-15T08:00:00' },
  ];

  grievances: Grievance[] = [
    { id: 1, ticketId: 'GRV-2024-001', applicantName: 'Ramsing Marak', phoneNumber: '9876543210', district: 'West Garo Hills', constituency: 'Ampati', category: 'INFRASTRUCTURE', subject: 'Road repair request – Dalu village', description: 'The main road connecting Dalu village to the district headquarters has been damaged since the last monsoon. Immediate repair is requested.', status: 'UNDER_REVIEW', submittedAt: '2024-03-10T09:00:00', assignedDepartment: 'PWD', remarks: 'Forwarded to PWD for inspection.' },
    { id: 2, ticketId: 'GRV-2024-002', applicantName: 'Sunita Sangma', phoneNumber: '9876500001', district: 'East Khasi Hills', constituency: 'Shillong East', category: 'EDUCATION', subject: 'School infrastructure improvement', description: 'Classroom walls are crumbling and there is no proper drinking water facility. Students are suffering.', status: 'FORWARDED', submittedAt: '2024-03-11T11:30:00', assignedDepartment: 'Education Dept' },
    { id: 3, ticketId: 'GRV-2024-003', applicantName: 'Bijoy Momin', phoneNumber: '9812345678', district: 'South Garo Hills', constituency: 'Baghmara', category: 'HEALTH', subject: 'PHC non-functional for 2 months', description: 'The Primary Health Centre at Baghmara Block has been without a doctor for 2 months. Villagers have no medical access.', status: 'RESOLVED', submittedAt: '2024-02-20T08:00:00', resolvedAt: '2024-03-05T16:00:00', assignedDepartment: 'Health Dept', remarks: 'Doctor posted. PHC operational.' },
    { id: 4, ticketId: 'GRV-2024-004', applicantName: 'Deibok Lyngdoh', phoneNumber: '9887654321', district: 'Ri Bhoi', constituency: 'Umsning', category: 'WELFARE_SCHEME', subject: 'Delay in CM Care disbursement', description: 'My CM Care application was approved 3 months ago but the amount has not been credited yet.', status: 'SUBMITTED', submittedAt: '2024-03-14T14:00:00' },
  ];
}
