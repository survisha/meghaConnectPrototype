import { Appointment } from '../models';
import { canRejectScheduled, canScheduleOrReschedule, isLiveWalkIn } from './appointment-action-policy';

describe('appointment action policy', () => {
  const appt = (category: 'SCHEDULED'|'WALK_IN', status: any) =>
    ({ appointmentCategory: category, status, isWalkIn: category === 'WALK_IN' } as Appointment);

  it('allows scheduled pending processing', () => {
    expect(canRejectScheduled(appt('SCHEDULED', 'PENDING'))).toBeTrue();
    expect(canScheduleOrReschedule(appt('SCHEDULED', 'PENDING'))).toBeTrue();
  });

  it('never exposes schedule or reject for a pending walk-in', () => {
    const walkIn = appt('WALK_IN', 'PENDING');
    expect(isLiveWalkIn(walkIn)).toBeTrue();
    expect(canRejectScheduled(walkIn)).toBeFalse();
    expect(canScheduleOrReschedule(walkIn)).toBeFalse();
  });

  it('keeps completed walk-ins read-only', () => {
    const walkIn = appt('WALK_IN', 'COMPLETED');
    expect(isLiveWalkIn(walkIn)).toBeFalse();
    expect(canScheduleOrReschedule(walkIn)).toBeFalse();
  });
});
