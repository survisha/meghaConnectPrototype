import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TimelineItem {
  timestamp: string;
  title: string;
  description?: string;
  status?: string;
  color?: string;
  icon?: string;
  performedBy?: string;
}

@Component({
  selector: 'app-timeline-view',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="timeline-container">
      <div *ngFor="let item of items; let last = last" class="timeline-item">
        <div class="timeline-left">
          <div class="timeline-dot" [style.background]="item.color || '#1a237e'">
            <i *ngIf="item.icon" [class]="'pi ' + item.icon" style="font-size:0.6rem;color:white"></i>
          </div>
          <div *ngIf="!last" class="timeline-line"></div>
        </div>
        <div class="timeline-content">
          <div class="timeline-header">
            <span class="timeline-title">{{ item.title }}</span>
            <span class="timeline-time">{{ item.timestamp | date:'dd MMM, h:mm a' }}</span>
          </div>
          <div *ngIf="item.description" class="timeline-desc">{{ item.description }}</div>
          <div *ngIf="item.performedBy" class="timeline-by">By: {{ item.performedBy }}</div>
        </div>
      </div>
      <div *ngIf="items.length === 0" style="text-align:center;color:#9ca3af;padding:1rem;font-size:0.85rem">
        No timeline entries yet.
      </div>
    </div>
  `,
  styles: [`
    .timeline-container { padding: 0.5rem 0; }
    .timeline-item { display: flex; gap: 0.75rem; }
    .timeline-left { display: flex; flex-direction: column; align-items: center; flex-shrink: 0; }
    .timeline-dot { width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
    .timeline-line { flex: 1; width: 2px; background: #e5e7eb; min-height: 20px; margin: 4px 0; }
    .timeline-content { flex: 1; padding-bottom: 1rem; }
    .timeline-header { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 0.25rem; }
    .timeline-title { font-weight: 600; color: #111827; font-size: 0.88rem; }
    .timeline-time { font-size: 0.75rem; color: #6b7280; }
    .timeline-desc { font-size: 0.82rem; color: #374151; margin-top: 0.25rem; }
    .timeline-by { font-size: 0.75rem; color: #9ca3af; margin-top: 0.2rem; font-style: italic; }
  `]
})
export class TimelineViewComponent {
  @Input() items: TimelineItem[] = [];
}
