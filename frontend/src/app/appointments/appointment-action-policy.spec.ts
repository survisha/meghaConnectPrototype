import { Appointment } from '../models';
import { canRejectScheduled, canReturnToPending, canScheduleOrReschedule, isLiveWalkIn } from './appointment-action-policy';

describe('appointment action policy', () => {
  const appt = (category: 'SCHEDULED'|'WALK_IN', status: any) =>
    ({ appointmentCategory: category, status, isWalkIn: category === 'WALK_IN' } as Appointment);

  it('allows scheduled pending processing', () => {
    expect(canRejectScheduled(appt('SCHEDULED', 'PENDING'))).toBeTrue();
    expect(canScheduleOrReschedule(appt('SCHEDULED', 'PENDING'))).toBeTrue();
  });

  it('allows rejecting a pending walk-in without exposing schedule', () => {
    const walkIn = appt('WALK_IN', 'PENDING');
    expect(isLiveWalkIn(walkIn)).toBeTrue();
    expect(canRejectScheduled(walkIn)).toBeTrue();
    expect(canScheduleOrReschedule(walkIn)).toBeFalse();
  });

  it('allows eligible scheduled appointments to return to pending regardless of event type', () => {
    expect(canReturnToPending(appt('SCHEDULED', 'SCHEDULED'))).toBeTrue();
    expect(canReturnToPending(appt('SCHEDULED', 'COMPLETED'))).toBeFalse();
  });

  it('keeps completed walk-ins read-only', () => {
    const walkIn = appt('WALK_IN', 'COMPLETED');
    expect(isLiveWalkIn(walkIn)).toBeFalse();
    expect(canScheduleOrReschedule(walkIn)).toBeFalse();
  });
});
