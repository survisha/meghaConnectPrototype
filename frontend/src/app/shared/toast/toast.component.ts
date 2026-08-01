import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { ToastNotification, ToastService } from './toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.scss']
})
export class ToastComponent implements OnInit, OnDestroy {
  toast: ToastNotification | null = null;
  private dismissTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly destroyed = new Subject<void>();
  private readonly browser: boolean;

  constructor(
    private readonly toastService: ToastService,
    private readonly cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) platformId: object
  ) {
    this.browser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    this.toastService.toast$.pipe(takeUntil(this.destroyed)).subscribe(toast => {
      this.clearDismissTimer();
      this.toast = toast;
      if (this.browser && toast.durationMs > 0) {
        this.dismissTimer = setTimeout(() => this.close(), toast.durationMs);
      }
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.clearDismissTimer();
    this.destroyed.next();
    this.destroyed.complete();
  }

  close(): void {
    const id = this.toast?.id;
    this.clearDismissTimer();
    this.toast = null;
    this.cdr.markForCheck();
    if (id !== undefined) this.toastService.dismiss(id);
  }

  private clearDismissTimer(): void {
    if (this.dismissTimer !== null) clearTimeout(this.dismissTimer);
    this.dismissTimer = null;
  }
}
