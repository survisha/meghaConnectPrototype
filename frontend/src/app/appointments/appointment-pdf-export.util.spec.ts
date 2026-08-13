import { Appointment } from '../models';
import { appointmentRemarkExportFields } from './appointment-pdf-export.util';

describe('appointmentRemarkExportFields', () => {
  const appointment = (values: Partial<Appointment>) => values as Appointment;

  it('keeps Approver remarks, forwarded department and HCM remarks independent', () => {
    expect(appointmentRemarkExportFields(appointment({
      approverRemarks: 'Approver verified the petition',
      department: 'Rural Development',
      hcmRemarks: 'HCM directed verification within 15 days',
    }))).toEqual({
      approverRemarks: 'Approver verified the petition',
      forwardedDepartment: 'Rural Development',
      hcmRemarks: 'HCM directed verification within 15 days',
    });
  });

  it('shows explicit placeholders when either or both remark values are missing', () => {
    expect(appointmentRemarkExportFields(appointment({ approverRemarks: 'Approver only' }))).toEqual({
      approverRemarks: 'Approver only',
      forwardedDepartment: 'Not Available',
      hcmRemarks: 'Not Available',
    });
    expect(appointmentRemarkExportFields(appointment({ hcmRemarks: 'HCM only' }))).toEqual({
      approverRemarks: 'Not Available',
      forwardedDepartment: 'Not Available',
      hcmRemarks: 'HCM only',
    });
    expect(appointmentRemarkExportFields(appointment({}))).toEqual({
      approverRemarks: 'Not Available',
      forwardedDepartment: 'Not Available',
      hcmRemarks: 'Not Available',
    });
  });
});
