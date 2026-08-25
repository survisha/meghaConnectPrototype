import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PublicIdentificationHistory, VisitorSearchService } from '../services/visitor-search.service';

@Component({
  selector: 'app-visitor-history-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title>Visitor History</h2>
    <mat-dialog-content>
      <div class="loading" *ngIf="loading"><mat-spinner diameter="36"></mat-spinner></div>
      <p class="error" *ngIf="error">{{error}}</p>
      <ng-container *ngIf="history as h">
        <div class="summary"><mat-icon>person</mat-icon><div><strong>{{h.citizenName || data.name || 'Visitor'}}</strong><span>{{h.visitCount || 0}} visit(s) · Last visited: {{h.lastVisitedAt ? (h.lastVisitedAt | date:'medium') : '—'}}</span></div></div>
        <h3>Appointment / Meeting History</h3>
        <div class="record" *ngFor="let a of h.appointments"><strong>{{a.applicationId || a.appointmentId || 'Appointment'}}</strong><span>{{a.status || '—'}} · {{a.appointmentDate | date:'medium'}}</span><p>{{a.purpose || a.department || '—'}}</p></div>
        <p *ngIf="!h.appointments?.length">No previous appointments.</p>
        <h3>Scheme Application History</h3>
        <div class="record" *ngFor="let s of h.schemes"><strong>{{s.schemeName || 'Scheme'}}</strong><span>{{s.status || '—'}} · {{s.appliedDate | date:'mediumDate'}}</span></div>
        <p *ngIf="!h.schemes?.length">No scheme applications.</p>
      </ng-container>
    </mat-dialog-content>
    <mat-dialog-actions align="end"><button mat-button mat-dialog-close>Close</button></mat-dialog-actions>`,
  styles: [`.loading{display:grid;place-items:center;padding:2rem}.error{color:#b91c1c}.summary{display:flex;gap:.75rem;align-items:center;padding:1rem;background:#eef2ff;border-radius:10px}.summary div,.summary span,.record span{display:block}.summary span,.record span{color:#64748b;font-size:.85rem}h3{margin:1.25rem 0 .5rem}.record{padding:.75rem;margin:.5rem 0;border:1px solid #e2e8f0;border-radius:8px}.record p{margin:.35rem 0 0}`]
})
export class VisitorHistoryDialogComponent implements OnInit {
  loading = true; error = ''; history?: PublicIdentificationHistory;
  constructor(@Inject(MAT_DIALOG_DATA) public data:{citizenId:number;name?:string}, private historyService:VisitorSearchService) {}
  ngOnInit(){this.historyService.getPublicIdentificationHistory(this.data.citizenId).subscribe({next:h=>{this.history=h;this.loading=false;},error:()=>{this.error='Unable to load visitor history.';this.loading=false;}});}
}
