import {
  Component, OnInit, OnDestroy, HostListener, ChangeDetectorRef, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil } from 'rxjs';
import { CalendarService, CalendarEvent } from '../services/calendar.service';
import { ScheduleEventService } from '../services/schedule-event.service';
import { CalendarEventDialogComponent } from './calendar-event-dialog.component';

export type ViewMode = 'day' | 'week' | 'month';

const DAY_NAMES = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTH_NAMES = [
  'January','February','March','April','May','June',
  'July','August','September','October','November','December'
];
const HOURS = Array.from({ length: 13 }, (_, i) => i + 8); // 08:00 – 20:00

interface WeekDay { date: Date; label: string; num: number; isToday: boolean; }
interface MonthCell { date: Date; dayNum: number; isCurrentMonth: boolean; isToday: boolean; events: CalendarEvent[]; }

@Component({
  selector: 'app-calendar-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule, MatTooltipModule,
    CalendarEventDialogComponent,
  ],
  templateUrl: './calendar-dashboard.component.html',
  styleUrls: ['./calendar-dashboard.component.scss'],
})
export class CalendarDashboardComponent implements OnInit, OnDestroy {

  /* ── State ── */
  viewMode: ViewMode = 'day';
  currentDate = new Date();
  events: CalendarEvent[] = [];
  loading = false;

  /* ── Computed grid data ── */
  hours = HOURS;
  weekDays: WeekDay[] = [];
  monthCells: MonthCell[] = [];

  /* ── Dialogs / panels ── */
  showEventDialog = false;
  editingEvent: CalendarEvent | null = null;
  preselectedDate = '';
  selectedEvent: CalendarEvent | null = null;
  showDetailPanel = false;

  /* ── Drag state ── */
  draggingEvent: CalendarEvent | null = null;
  dragGhostX = 0;
  dragGhostY = 0;
  dragOffsetX = 0;
  dragOffsetY = 0;

  private destroy$ = new Subject<void>();

  readonly typeColors: Record<string, string> = {
    A1: '#1565c0', A2: '#2e7d32', A3: '#f57f17',
    A4: '#c62828', B1: '#4527a0', B2: '#006064',
  };
  readonly statusColors: Record<string, string> = {
    approved: '#16a34a', pending: '#ca8a04',
    conflict: '#dc2626', public: '#2563eb',
  };
  readonly legend = [
    { label: 'A1: Cabinet/Flight', color: '#1565c0' },
    { label: 'A2: Events',         color: '#2e7d32' },
    { label: 'A3: Files',          color: '#f57f17' },
    { label: 'A4: Appointments',   color: '#c62828' },
    { label: 'B1: Public Durbar',  color: '#4527a0' },
    { label: 'B2: Walk-in',        color: '#006064' },
  ];

  constructor(
    private calendarService: CalendarService,
    private scheduleEventService: ScheduleEventService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() { this.loadEvents(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  /* ─── Loading ─────────────────────────────────────────────────── */
  private loadEvents() {
    this.loading = true;
    this.scheduleEventService.getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (evs) => {
          const mapped: CalendarEvent[] = evs.map(e => ({ ...e }));
          this.events = this.calendarService.detectConflicts(mapped);
          this.rebuildGrid();
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.loading = false;
          this.cdr.markForCheck();
        }
      });
  }

  /* ─── Navigation ──────────────────────────────────────────────── */
  navigate(dir: -1 | 1) {
    const d = new Date(this.currentDate);
    if (this.viewMode === 'day')   d.setDate(d.getDate() + dir);
    if (this.viewMode === 'week')  d.setDate(d.getDate() + dir * 7);
    if (this.viewMode === 'month') d.setMonth(d.getMonth() + dir);
    this.currentDate = d;
    this.rebuildGrid();
    this.cdr.markForCheck();
  }

  goToday() {
    this.currentDate = new Date();
    this.rebuildGrid();
    this.cdr.markForCheck();
  }

  setView(mode: ViewMode) {
    this.viewMode = mode;
    this.rebuildGrid();
    this.cdr.markForCheck();
  }

  /* ─── Date label ─────────────────────────────────────────────── */
  get dateLabel(): string {
    const d = this.currentDate;
    if (this.viewMode === 'day')
      return `${DAY_NAMES[d.getDay()]}, ${d.getDate()} ${MONTH_NAMES[d.getMonth()]} ${d.getFullYear()}`;
    if (this.viewMode === 'week') {
      const start = this.weekDays[0]?.date;
      const end   = this.weekDays[6]?.date;
      if (!start || !end) return '';
      return `${start.getDate()} – ${end.getDate()} ${MONTH_NAMES[d.getMonth()]} ${d.getFullYear()}`;
    }
    return `${MONTH_NAMES[d.getMonth()]} ${d.getFullYear()}`;
  }

  /* ─── Grid builders ───────────────────────────────────────────── */
  private rebuildGrid() {
    if (this.viewMode === 'week')  this.buildWeek();
    if (this.viewMode === 'month') this.buildMonth();
  }

  private buildWeek() {
    const today = new Date();
    today.setHours(0,0,0,0);
    const dow = this.currentDate.getDay();
    const sunday = new Date(this.currentDate);
    sunday.setDate(sunday.getDate() - dow);
    sunday.setHours(0,0,0,0);
    this.weekDays = Array.from({ length: 7 }, (_, i) => {
      const d = new Date(sunday);
      d.setDate(d.getDate() + i);
      return { date: d, label: DAY_NAMES[i], num: d.getDate(), isToday: d.getTime() === today.getTime() };
    });
  }

  private buildMonth() {
    const today = new Date();
    today.setHours(0,0,0,0);
    const year  = this.currentDate.getFullYear();
    const month = this.currentDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const startOffset = firstDay.getDay();
    this.monthCells = [];
    for (let i = 0; i < 42; i++) {
      const d = new Date(year, month, 1 - startOffset + i);
      d.setHours(0,0,0,0);
      const isCurrent = d.getMonth() === month;
      const isToday   = d.getTime() === today.getTime();
      const dayEvs = this.getEventsForDate(d);
      this.monthCells.push({ date: d, dayNum: d.getDate(), isCurrentMonth: isCurrent, isToday, events: dayEvs });
    }
  }

  /* ─── Event queries ───────────────────────────────────────────── */
  getEventsForHour(hour: number, date?: Date): CalendarEvent[] {
    const d = date ?? this.currentDate;
    return this.events.filter(e => {
      const s = new Date(e.startTime);
      return s.getFullYear() === d.getFullYear() &&
             s.getMonth()    === d.getMonth()    &&
             s.getDate()     === d.getDate()     &&
             s.getHours()    === hour;
    });
  }

  getEventsForDate(d: Date): CalendarEvent[] {
    return this.events.filter(e => {
      const s = new Date(e.startTime);
      return s.getFullYear() === d.getFullYear() &&
             s.getMonth()    === d.getMonth()    &&
             s.getDate()     === d.getDate();
    });
  }

  /* ─── Colors ─────────────────────────────────────────────────── */
  getChipColor(ev: CalendarEvent): string {
    if (ev.isConflict) return '#dc2626';
    if (ev.status && this.statusColors[ev.status]) return this.statusColors[ev.status];
    return this.typeColors[ev.eventType] ?? '#6b7280';
  }

  /* ─── Time helpers ────────────────────────────────────────────── */
  formatTime(dt: string): string {
    return new Date(dt).toLocaleTimeString('en-IN', { hour:'2-digit', minute:'2-digit', hour12:true });
  }

  hourLabel(h: number): string {
    return `${String(h).padStart(2,'0')}:00`;
  }

  /* ─── Event detail ────────────────────────────────────────────── */
  openDetail(ev: CalendarEvent, event: MouseEvent) {
    event.stopPropagation();
    this.selectedEvent = ev;
    this.showDetailPanel = true;
    this.cdr.markForCheck();
  }

  closeDetail() { this.showDetailPanel = false; this.cdr.markForCheck(); }

  /* ─── Create / Edit ───────────────────────────────────────────── */
  openCreateDialog(date?: Date) {
    this.editingEvent  = null;
    this.preselectedDate = date
      ? new Date(date).toISOString().slice(0,16)
      : this.toLocalIso(this.currentDate);
    this.showEventDialog = true;
    this.cdr.markForCheck();
  }

  openEditDialog(ev: CalendarEvent, e: MouseEvent) {
    e.stopPropagation();
    this.editingEvent    = ev;
    this.preselectedDate = ev.startTime;
    this.showEventDialog = true;
    this.cdr.markForCheck();
  }

  onEventSaved(payload: Partial<CalendarEvent>) {
    const obs = this.editingEvent?.id
      ? this.scheduleEventService.update(this.editingEvent.id, payload)
      : this.scheduleEventService.create(payload);

    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: (saved) => {
        if (this.editingEvent?.id) {
          const idx = this.events.findIndex(e => e.id === saved.id);
          if (idx !== -1) this.events[idx] = { ...saved };
        } else {
          this.events.push({ ...saved });
        }
        this.events = this.calendarService.detectConflicts([...this.events]);
        this.rebuildGrid();
        this.showEventDialog = false;
        this.cdr.markForCheck();
      },
      error: () => { this.showEventDialog = false; this.cdr.markForCheck(); }
    });
  }

  onEventCancelled() {
    this.showEventDialog = false;
    this.cdr.markForCheck();
  }

  deleteEvent(ev: CalendarEvent, e: MouseEvent) {
    e.stopPropagation();
    if (!confirm(`Delete "${ev.title}"?`)) return;
    this.scheduleEventService.delete(ev.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.events = this.calendarService.detectConflicts(this.events.filter(x => x.id !== ev.id));
        this.rebuildGrid();
        if (this.selectedEvent?.id === ev.id) this.showDetailPanel = false;
        this.cdr.markForCheck();
      });
  }

  /* ─── Drag & Drop (mouse) ─────────────────────────────────────── */
  onDragStart(ev: CalendarEvent, e: MouseEvent) {
    e.preventDefault();
    this.draggingEvent = ev;
    this.dragOffsetX   = 12;
    this.dragOffsetY   = 12;
    this.dragGhostX    = e.clientX + this.dragOffsetX;
    this.dragGhostY    = e.clientY + this.dragOffsetY;
    this.cdr.markForCheck();
  }

  @HostListener('document:mousemove', ['$event'])
  onMouseMove(e: MouseEvent) {
    if (!this.draggingEvent) return;
    this.dragGhostX = e.clientX + this.dragOffsetX;
    this.dragGhostY = e.clientY + this.dragOffsetY;
    this.cdr.markForCheck();
  }

  @HostListener('document:mouseup')
  onMouseUp() {
    if (this.draggingEvent) {
      this.draggingEvent = null;
      this.cdr.markForCheck();
    }
  }

  /* ─── Month cell click ────────────────────────────────────────── */
  onMonthCellClick(cell: MonthCell) {
    this.viewMode    = 'day';
    this.currentDate = new Date(cell.date);
    this.cdr.markForCheck();
  }

  /* ─── Today check ─────────────────────────────────────────────── */
  isToday(d: Date): boolean {
    const t = new Date();
    return d.getDate() === t.getDate() &&
           d.getMonth() === t.getMonth() &&
           d.getFullYear() === t.getFullYear();
  }

  /* ─── Util ────────────────────────────────────────────────────── */
  private toLocalIso(d: Date): string {
    const pad = (n: number) => String(n).padStart(2,'0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  trackById(_: number, ev: CalendarEvent) { return ev.id; }
  trackByHour(_: number, h: number)       { return h; }
  trackByDate(_: number, c: MonthCell)    { return c.date.toISOString(); }
}
