import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastOptions {
  durationMs?: number;
  dismissible?: boolean;
  title?: string;
}

interface LegacyNotificationOptions {
  duration?: number;
  panelClass?: string | string[];
}

export interface ToastNotification {
  id: number;
  message: string;
  type: ToastType;
  durationMs: number;
  dismissible: boolean;
  createdAt: number;
  title?: string;
}

const DEFAULT_DURATIONS: Record<ToastType, number> = {
  success: 3000,
  info: 3500,
  warning: 5000,
  error: 6000,
};

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly notifications = new Subject<ToastNotification>();
  private readonly queue: ToastNotification[] = [];
  private sequence = 0;
  private active: ToastNotification | null = null;
  private lastToast: { message: string; type: ToastType; shownAt: number } | null = null;
  private readonly maxQueueSize = 10;
  private readonly duplicateWindowMs = 1000;

  readonly toast$: Observable<ToastNotification> = this.notifications.asObservable();

  success(message: string, options?: ToastOptions): void { this.showToast(message, 'success', options); }
  error(message: string, options?: ToastOptions): void { this.showToast(message, 'error', options); }
  info(message: string, options?: ToastOptions): void { this.showToast(message, 'info', options); }
  warning(message: string, options?: ToastOptions): void { this.showToast(message, 'warning', options); }

  /** Migration-compatible entry point for former MatSnackBar callers. */
  open(message: string, _action?: string, options: LegacyNotificationOptions = {}): void {
    const classes = Array.isArray(options.panelClass) ? options.panelClass : [options.panelClass ?? ''];
    const className = classes.join(' ').toLowerCase();
    const type: ToastType = className.includes('error') ? 'error'
      : className.includes('warn') ? 'warning'
      : className.includes('success') ? 'success' : 'info';
    this.showToast(message, type, { durationMs: options.duration });
  }

  showToast(message: string, type: ToastType, options: ToastOptions = {}): void {
    const normalized = message?.trim();
    if (!normalized) return;

    const now = Date.now();
    if (this.lastToast?.message === normalized && this.lastToast.type === type &&
        now - this.lastToast.shownAt < this.duplicateWindowMs) return;
    this.lastToast = { message: normalized, type, shownAt: now };

    const toast: ToastNotification = {
      id: ++this.sequence,
      message: normalized,
      type,
      durationMs: options.durationMs ?? DEFAULT_DURATIONS[type],
      dismissible: options.dismissible ?? true,
      createdAt: now,
      title: options.title?.trim() || undefined,
    };

    if (!this.active) {
      this.activate(toast);
    } else if (this.queue.length < this.maxQueueSize) {
      this.queue.push(toast);
    }
  }

  dismiss(id?: number): void {
    if (id !== undefined && this.active?.id !== id) return;
    this.active = null;
    const next = this.queue.shift();
    if (next) this.activate(next);
  }

  clearAll(): void {
    this.queue.length = 0;
    this.active = null;
  }

  private activate(toast: ToastNotification): void {
    this.active = toast;
    this.notifications.next(toast);
  }
}
