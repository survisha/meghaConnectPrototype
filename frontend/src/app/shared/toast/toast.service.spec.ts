import { fakeAsync, tick } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => service = new ToastService());

  it('emits each severity with trimmed text and increasing IDs', () => {
    const emitted: Array<{ id: number; message: string; type: string }> = [];
    service.toast$.subscribe(toast => emitted.push(toast));
    service.success('  Saved  ');
    service.dismiss();
    service.error('Failed');
    service.dismiss();
    service.warning('Review');
    service.dismiss();
    service.info('Queued');
    expect(emitted.map(item => item.type)).toEqual(['success', 'error', 'warning', 'info']);
    expect(emitted.map(item => item.id)).toEqual([1, 2, 3, 4]);
    expect(emitted[0].message).toBe('Saved');
  });

  it('ignores blank messages', () => {
    const emitted = jasmine.createSpy('emitted');
    service.toast$.subscribe(emitted);
    service.error('   ');
    expect(emitted).not.toHaveBeenCalled();
  });

  it('suppresses an exact duplicate within the duplicate window', () => {
    const emitted = jasmine.createSpy('emitted');
    service.toast$.subscribe(emitted);
    service.error('Unavailable');
    service.error('Unavailable');
    expect(emitted).toHaveBeenCalledTimes(1);
  });

  it('allows the same message with another severity', () => {
    const emitted = jasmine.createSpy('emitted');
    service.toast$.subscribe(emitted);
    service.error('Complete');
    service.dismiss();
    service.success('Complete');
    expect(emitted).toHaveBeenCalledTimes(2);
  });

  it('allows the same message after the duplicate window', fakeAsync(() => {
    const emitted = jasmine.createSpy('emitted');
    service.toast$.subscribe(emitted);
    service.info('Refreshed');
    service.dismiss();
    tick(1001);
    service.info('Refreshed');
    expect(emitted).toHaveBeenCalledTimes(2);
  }));

  it('queues notifications and displays them in order', () => {
    const messages: string[] = [];
    service.toast$.subscribe(toast => messages.push(toast.message));
    service.info('One');
    service.warning('Two');
    service.error('Three');
    expect(messages).toEqual(['One']);
    service.dismiss();
    service.dismiss();
    expect(messages).toEqual(['One', 'Two', 'Three']);
  });
});
