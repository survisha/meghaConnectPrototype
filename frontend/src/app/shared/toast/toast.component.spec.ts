import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ToastComponent } from './toast.component';
import { ToastService } from './toast.service';

describe('ToastComponent', () => {
  let fixture: ComponentFixture<ToastComponent>;
  let service: ToastService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ToastComponent] }).compileComponents();
    fixture = TestBed.createComponent(ToastComponent);
    service = TestBed.inject(ToastService);
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('renders the active severity, accessible role, and long message', () => {
    service.warning('A long warning that remains readable on narrow screens.');
    fixture.detectChanges();
    const toast = fixture.nativeElement.querySelector('.toast') as HTMLElement;
    expect(toast.classList).toContain('warning');
    expect(toast.getAttribute('role')).toBe('status');
    expect(toast.textContent).toContain('A long warning');
  });

  it('closes manually and displays the next queued toast', () => {
    service.success('First');
    service.info('Second');
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.toast-close') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Second');
  });

  it('auto-dismisses using the configured duration', fakeAsync(() => {
    service.info('Short', { durationMs: 100 });
    fixture.detectChanges();
    tick(101);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.toast')).toBeNull();
  }));

  it('clears its timer on destroy', fakeAsync(() => {
    service.error('Destroy safely', { durationMs: 100 });
    fixture.detectChanges();
    fixture.destroy();
    tick(101);
    expect().nothing();
  }));
});
