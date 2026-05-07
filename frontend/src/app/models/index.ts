export type UserRole =
  | 'HCM' | 'ADMIN' | 'OSD'
  | 'APPROVER' | 'CMO_OFFICER'
  | 'DATA_ENTRY_OPERATOR' | 'PUBLIC';

export type EventType = 'A1' | 'A2' | 'A3' | 'A4' | 'B1' | 'B2';
export type Location  = 'SHILLONG' | 'TURA' | 'DELHI' | 'OTHERS';
export type SchemeType = 'CMSDF' | 'CMSG' | 'CM_CARE' | 'CM_CONNECT' | 'CM_ELEVATE' | 'FOCUS_PLUS' | 'OTHERS';
export type DirectionColor = 'GREEN' | 'YELLOW' | 'BLUE';
export type AppointmentStatus =
  | 'CREATED' | 'SUBMITTED' | 'DEO_PROCESSED' | 'PENDING_APPROVER_REVIEW'
  | 'CMO_REVIEW' | 'APPROVER_REVIEW' | 'HCM_PENDING'
  | 'HCM_ACCEPTED' | 'HCM_SNOOZED' | 'HCM_REJECTED'
  | 'SELECTED_FOR_PUBLIC_DARBAR' | 'PUBLIC_DARBAR_DATE_CREATED'
  | 'SCHEDULED_FOR_PUBLIC_DARBAR' | 'APPROVED_WITH_DATE_TIME'
  | 'REJECTED' | 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export type KycStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export type DocumentType = 
  | 'EPIC_SCAN' | 'APPLICATION_LETTER' | 'PLANS_ESTIMATES' 
  | 'BANK_DETAILS' | 'MLA_APPROVAL_LETTER' | 'ORG_REGISTRATION_CERTIFICATE'
  | 'CM_CARE_ELIGIBILITY' | 'CM_CARE_HOSPITAL' | 'CM_CARE_SUPPORTING';

export interface Visitor {
  id: number;
  fullName: string;
  phoneNumber: string;
  epicNumber: string;
  photoUrl?: string;
  designation: string;
  address?: string;
  fullAddress?: string;
  address1?: string;
  addressLine?: string;
  city?: string;
  state?: string;
  pincode?: string;
  district: string;
  constituency: string;
  booth: string;
  village?: string;
  briefProfile?: string;
  kycStatus?: KycStatus;
}

export interface AppointmentDocument {
  id?: number;
  appointmentId?: number;
  documentType: DocumentType;
  fileName: string;
  filePath: string;
  fileSize: number;
  uploadedAt?: string;
  isRequired: boolean;
  status: 'UPLOADED' | 'PENDING' | 'VERIFIED';
}

export interface Appointment {
  id: number;
  applicationId: string;
  applicantId?: number;
  applicant: Visitor;
  applicantName?: string;
  applicantPhone?: string;
  subject?: string;
  department?: string;
  appointmentType?: string;
  agendaType: string;
  agendaBrief: string;
  status: AppointmentStatus;
  requestedLocation: Location;
  scheduledDateTime?: string;
  scheduledDurationMinutes?: number;
  eventType: EventType;
  mlaMdcApproved?: boolean;
  meetingCountLast6Months?: number;
  cmoRemarks?: string;
  approverRemarks?: string;
  hcmRemarks?: string;
  shortNotes?: string;
  directions?: Direction[];
  isWalkIn?: boolean;
  createdAt?: string;
  submittedAt?: string;
  updatedAt?: string;
}


export interface ScheduleEvent {
  id: number;
  title: string;
  eventType: EventType;
  startTime: string;
  endTime: string;
  location: Location;
  travelTimeMinutes?: number;
  isConflict?: boolean;
  description?: string;
  shortNotes?: string;
}

export interface SchemeApplication {
  id: number;
  appointmentId?: number;
  applicant: Visitor;
  schemeType: SchemeType;
  projectName: string;
  projectCategory: string;
  beneficiaryType: string;
  beneficiaryCount: string;
  estimatedCost: number;
  communityContribution: number;
  justification: string;
  hcmDecision?: string;
  hcmApprovedCost?: number;
  status: string;
}

export interface Direction {
  id: number;
  appointmentId: number;
  color: DirectionColor;
  directionText: string;
  assignedDepartment?: string;
  deadline?: string;
  currentStatus?: string;
  isCompleted: boolean;
}

export interface AuditEntry {
  id: number;
  entityType: string;
  entityId: number;
  action: string;
  details: string;
  performedBy: string;
  timestamp: string;
}

export type GrievanceCategory =
  | 'PUBLIC_SERVICES' | 'INFRASTRUCTURE' | 'HEALTH' | 'EDUCATION'
  | 'EMPLOYMENT' | 'WELFARE_SCHEME' | 'LAW_ORDER' | 'OTHERS';

export type GrievanceStatus =
  | 'SUBMITTED' | 'ACKNOWLEDGED' | 'UNDER_REVIEW'
  | 'FORWARDED' | 'RESOLVED' | 'CLOSED';

export interface Grievance {
  id: number;
  ticketId: string;
  applicantName: string;
  phoneNumber: string;
  district: string;
  constituency?: string;
  category: GrievanceCategory;
  subject: string;
  description: string;
  status: GrievanceStatus;
  submittedAt: string;
  resolvedAt?: string;
  assignedDepartment?: string;
  remarks?: string;
}
