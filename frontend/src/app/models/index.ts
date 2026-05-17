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
  | 'CMO_REVIEW' | 'APPROVER_REVIEW' | 'APPROVED' | 'HCM_PENDING'
  | 'HCM_ACCEPTED' | 'HCM_SNOOZED' | 'HCM_REJECTED'
  | 'FOLLOWUP' | 'SELECTED_FOR_PUBLIC_DARBAR' | 'PUBLIC_DARBAR_DATE_CREATED'
  | 'SCHEDULED_FOR_PUBLIC_DARBAR' | 'APPROVED_WITH_DATE_TIME'
  | 'REJECTED' | 'SCHEDULED' | 'FORWARDED_TO_DEPARTMENT'
  | 'SUPPORTING_DOCUMENT_REQUIRED' | 'COMPLETED' | 'CANCELLED';

export type KycStatus = 'PENDING' | 'KYC_PENDING' | 'PHOTO_MATCHED' | 'DEMOGRAPHIC_MATCHED' | 'FAILED' | 'MANUAL_VERIFICATION_REQUIRED' | 'VERIFIED' | 'REJECTED';

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
  livePhotoBase64?: string;
  photoBase64?: string;
  livePhotoPath?: string;
  photoStoragePath?: string;
  photoPath?: string;
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
  boothVillage?: string;
  village?: string;
  location?: string;
  briefProfile?: string;
  gender?: string;
  dateOfBirth?: string;
  outsideMeghalaya?: boolean;
  kycStatus?: KycStatus;
  kycType?: string;
  kycProvider?: string;
  kycFailureReason?: string;
  kycRequestId?: string;
}

export interface AppointmentDocument {
  id?: number;
  appointmentId?: number;
  documentType: DocumentType;
  fileName: string;
  filePath?: string;
  fileSize: number;
  mimeType?: string;
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
  appointmentSource?: 'CITIZEN' | 'GUEST' | string;
  guestReferenceId?: string;
  guestName?: string;
  guestMobile?: string;
  guestAddress?: string;
  guestEmail?: string;
  organizationName?: string;
  guestDesignation?: string;
  visitorCategory?: string;
  referredOffice?: string;
  referredByName?: string;
  reasonForAppointment?: string;
  preferredDate?: string;
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
  associates?: AssociateCitizen[];
  isWalkIn?: boolean;
  createdAt?: string;
  submittedAt?: string;
  updatedAt?: string;
}

export interface AssociateCitizen {
  id?: number;
  citizenId: number;
  fullName: string;
  mobileNumber?: string;
  epicReference?: string;
  aadhaarReference?: string;
  addressSummary?: string;
  photoUrl?: string;
  kycStatus?: string;
  status?: string;
  relationship?: string;
  remarks?: string;
  role?: string;
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
  sourceType?: 'SCHEDULE_EVENT' | 'APPOINTMENT';
  sourceId?: number;
  appointmentId?: number;
  appointment?: Appointment;
  appointments?: Appointment[];
  appointmentCount?: number;
}

export interface SchemeApplication {
  id: number;
  appointmentId?: number;
  applicantId?: number;
  applicantName?: string;
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
  items?: SchemeApplicationItem[];
  createdAt?: string;
  updatedAt?: string;
}

export interface SchemeApplicationItem {
  id?: number;
  description: string;
  quantity: number;
  unitCost: number;
  cmoModeratedUnitCost?: number;
  hcmApprovedUnitCost?: number;
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
  module?: string;
  entity?: string;
  entityType: string;
  entityId: number;
  action: string;
  user?: string;
  details: string;
  description?: string;
  performedBy: string;
  role?: string;
  userRole?: string;
  requestId?: string;
  oldValue?: string;
  newValue?: string;
  status?: string;
  ipAddress?: string;
  endpoint?: string;
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
  visitorId?: number;
  ticketId: string;
  applicantName: string;
  phoneNumber: string;
  district: string;
  constituency?: string;
  visitorDesignation?: string;
  category: GrievanceCategory;
  subject: string;
  description: string;
  status: GrievanceStatus;
  submittedAt: string;
  resolvedAt?: string;
  assignedDepartment?: string;
  remarks?: string;
}
