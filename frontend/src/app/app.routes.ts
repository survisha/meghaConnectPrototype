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
import { UserManagementComponent } from './admin/user-management.component';
import { GrievancesComponent } from './grievances/grievances.component';
import { VisitorDashboardComponent } from './visitor-dashboard/visitor-dashboard.component';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';

import { UserRole } from './models';

const FULL_CONTROL: UserRole[] = ['HCM', 'ADMIN', 'SAIDUL_OSD'];
const STAFF_ROLES: UserRole[] = ['HCM', 'ADMIN', 'SAIDUL_OSD', 'APPROVER_JT_SECY', 'CMO_OFFICER', 'DATA_ENTRY_OPERATOR'];
const REPORTS_ROLES: UserRole[] = ['HCM', 'ADMIN', 'SAIDUL_OSD', 'APPROVER_JT_SECY', 'CMO_OFFICER'];

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '', component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'visitor', component: VisitorDashboardComponent, canActivate: [roleGuard('PUBLIC')] },
      { path: 'scheduling', component: SchedulingComponent, canActivate: [roleGuard(...REPORTS_ROLES)] },
      { path: 'appointments', component: AppointmentListComponent, canActivate: [roleGuard(...STAFF_ROLES)] },
      { path: 'appointments/new', component: AppointmentFormComponent },
      { path: 'appointments/walkin', component: WalkinComponent, canActivate: [roleGuard('ADMIN', 'SAIDUL_OSD', 'DATA_ENTRY_OPERATOR')] },
      { path: 'appointments/:id', component: AppointmentDetailComponent, canActivate: [roleGuard(...STAFF_ROLES)] },
      { path: 'schemes', component: SchemeListComponent, canActivate: [roleGuard(...REPORTS_ROLES)] },
      { path: 'schemes/apply', component: SchemeFormComponent },
      { path: 'grievances', component: GrievancesComponent },
      { path: 'identify', component: PublicIdentificationComponent, canActivate: [roleGuard('HCM', 'ADMIN', 'SAIDUL_OSD', 'DATA_ENTRY_OPERATOR')] },
      { path: 'reports', component: ReportsComponent, canActivate: [roleGuard(...REPORTS_ROLES)] },
      { path: 'reports/heatmap', component: HeatmapComponent, canActivate: [roleGuard(...REPORTS_ROLES)] },
      { path: 'reports/followups', component: PendingFollowupsComponent, canActivate: [roleGuard(...REPORTS_ROLES)] },
      { path: 'reports/audit', component: AuditTrailComponent, canActivate: [roleGuard('ADMIN')] },
      { path: 'admin/users', component: UserManagementComponent, canActivate: [roleGuard(...FULL_CONTROL)] },
    ]
  },
  { path: '**', redirectTo: '' }
];
