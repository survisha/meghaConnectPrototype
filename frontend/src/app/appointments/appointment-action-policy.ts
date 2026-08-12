import { Appointment } from '../models';

export const isScheduledAppointment = (a: Appointment | null): boolean =>
  !!a && (a.appointmentCategory === 'SCHEDULED' || (!a.appointmentCategory && a.isWalkIn !== true));

export const isWalkInAppointment = (a: Appointment | null): boolean =>
  !!a && (a.appointmentCategory === 'WALK_IN' || a.isWalkIn === true);

export const canRejectScheduled = (a: Appointment | null): boolean =>
  isScheduledAppointment(a) && a?.status === 'PENDING';

export const canScheduleOrReschedule = (a: Appointment | null): boolean =>
  isScheduledAppointment(a) && !!a && ['PENDING', 'SCHEDULED'].includes(a.status);

export const isLiveWalkIn = (a: Appointment | null): boolean =>
  isWalkInAppointment(a) && a?.status === 'PENDING';
