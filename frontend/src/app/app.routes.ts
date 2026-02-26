import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { ShellComponent } from './shell/shell.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SchedulingComponent } from './scheduling/scheduling.component';
import { AppointmentListComponent } from './appointments/appointment-list/appointment-list.component';
import { AppointmentDetailComponent } from './appointments/appointment-detail/appointment-detail.component';
import { AppointmentFormComponent } from './appointments/appointment-form/appointment-form.component';
import { WalkinComponent } from './appointments/walkin/walkin.component';
import { SchemeListComponent } from './schemes/scheme-list/scheme-list.component';
import { SchemeFormComponent } from './schemes/scheme-form/scheme-form.component';
import { PublicIdentificationComponent } from './public-identification/public-identification.component';
import { ReportsComponent } from './reports/reports.component';
import { HeatmapComponent } from './reports/heatmap/heatmap.component';
import { PendingFollowupsComponent } from './reports/pending-followups/pending-followups.component';
import { AuditTrailComponent } from './reports/audit-trail/audit-trail.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '', component: ShellComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'scheduling', component: SchedulingComponent },
      { path: 'appointments', component: AppointmentListComponent },
      { path: 'appointments/new', component: AppointmentFormComponent },
      { path: 'appointments/walkin', component: WalkinComponent },
      { path: 'appointments/:id', component: AppointmentDetailComponent },
      { path: 'schemes', component: SchemeListComponent },
      { path: 'schemes/apply', component: SchemeFormComponent },
      { path: 'identify', component: PublicIdentificationComponent },
      { path: 'reports', component: ReportsComponent },
      { path: 'reports/heatmap', component: HeatmapComponent },
      { path: 'reports/followups', component: PendingFollowupsComponent },
      { path: 'reports/audit', component: AuditTrailComponent },
    ]
  },
  { path: '**', redirectTo: '' }
];
