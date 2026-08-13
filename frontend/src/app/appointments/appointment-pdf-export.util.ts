import { Appointment } from '../models';

export interface AppointmentRemarkExportFields {
  approverRemarks: string;
  forwardedDepartment: string;
  hcmRemarks: string;
}

export function appointmentRemarkExportFields(appointment: Appointment): AppointmentRemarkExportFields {
  return {
    approverRemarks: appointment.approverRemarks?.trim() || 'Not Available',
    forwardedDepartment: appointment.department?.trim() || 'Not Available',
    hcmRemarks: appointment.hcmRemarks?.trim() || 'Not Available',
  };
}
