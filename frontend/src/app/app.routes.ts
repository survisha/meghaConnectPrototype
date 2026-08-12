import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './auth/login/login.component';
import { ChangePasswordComponent } from './auth/change-password/change-password.component';
import { PublicLoginComponent } from './public-login/public-login.component';
import { GuestAppointmentComponent } from './guest-appointment/guest-appointment.component';
import { VisitorRegisterComponent } from './visitor-register/visitor-register.component';
import { ShellComponent } from './shell/shell.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SchedulingComponent } from './scheduling/scheduling.component';
import { AppointmentListComponent } from './appointments/appointment-list/appointment-list.component';
import { AppointmentFormComponent } from './appointments/appointment-form/appointment-form.component';
import { WalkinComponent } from './appointments/walkin/walkin.component';
import { SchemeListComponent } from './schemes/scheme-list/scheme-list.component';
import { SchemeFormComponent } from './schemes/scheme-form/scheme-form.component';
import { PublicIdentificationComponent } from './public-identification/public-identification.component';
import { ReportsComponent } from './reports/reports.component';
import { HeatmapComponent } from './reports/heatmap/heatmap.component';
import { AuditTrailComponent } from './reports/audit-trail/audit-trail.component';
import { CompletedAppointmentsComponent } from './completed-appointments/completed-appointments.component';
import { RejectedAppointmentsComponent } from './rejected-appointments/rejected-appointments.component';
import { UserManagementComponent } from './admin/user-management.component';
import { DepartmentManagementComponent } from './admin/departments/department-management.component';
import { DepartmentRequestsComponent } from './admin/department-requests/department-requests.component';
import { DepartmentAccessRequestFormComponent } from './department-access-request/department-access-request-form.component';
import { SchemeManagementComponent } from './admin/scheme-management/scheme-management.component';
import { AppointmentTypeManagementComponent } from './admin/appointment-type-management/appointment-type-management.component';
import { HcmDashboardComponent } from './admin/hcm-dashboard/hcm-dashboard.component';
import { GrievancesComponent } from './grievances/grievances.component';
import { VisitorDashboardComponent } from './visitor-dashboard/visitor-dashboard.component';
import { ApproverInboxComponent } from './approver-workflow/approver-inbox.component';
import { AppointmentApprovalDetailsComponent } from './approver-workflow/appointment-approval-details.component';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';
import { passwordChangeGuard } from './guards/password-change.guard';
import { featureAccessGuard } from './guards/feature-access.guard';

import { UserRole } from './models';

const REPORTS_ROLES: UserRole[] = ['SUPER_ADMIN', 'DEPARTMENT_PA', 'HCM', 'ADMIN', 'APPROVER'];

export const routes: Routes = [
  { path: '', component: HomeComponent, pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'change-password', component: ChangePasswordComponent, canActivate: [passwordChangeGuard] },
  { path: 'public-login', component: PublicLoginComponent },
  { path: 'register-visitor', component: VisitorRegisterComponent },
  { path: 'guest-appointment', component: GuestAppointmentComponent },
  { path: 'department-access-request', component: DepartmentAccessRequestFormComponent },
  {
    path: '', component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'visitor', component: VisitorDashboardComponent, canActivate: [roleGuard('PUBLIC')] },
      { path: 'scheduling', component: SchedulingComponent, canActivate: [featureAccessGuard('calendar')] },
      { path: 'appointments', component: AppointmentListComponent, canActivate: [featureAccessGuard('appointments')] },
      { path: 'appointments/new', component: AppointmentFormComponent, canActivate: [featureAccessGuard('appointments')] },
      { path: 'appointments/walkin', component: WalkinComponent, canActivate: [featureAccessGuard('walkIn')] },
      { path: 'appointments/pending-approvals', component: ApproverInboxComponent, canActivate: [roleGuard('SUPER_ADMIN', 'HCM', 'ADMIN', 'APPROVER')] },
      { path: 'appointments/approval-details/:id', component: AppointmentApprovalDetailsComponent, canActivate: [roleGuard('SUPER_ADMIN', 'HCM', 'ADMIN', 'APPROVER')] },
      { path: 'approver', redirectTo: 'appointments', pathMatch: 'full' },
      { path: 'schemes', component: SchemeListComponent, canActivate: [roleGuard(...REPORTS_ROLES)] },
      { path: 'schemes/apply', component: SchemeFormComponent, canActivate: [roleGuard('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'PUBLIC')] },
      { path: 'grievances', component: GrievancesComponent },
      { path: 'deo/register-visitor', component: VisitorRegisterComponent, canActivate: [featureAccessGuard('registerVisitor')] },
      { path: 'identify', component: PublicIdentificationComponent, canActivate: [featureAccessGuard('publicIdentification')] },
      { path: 'reports', component: ReportsComponent, canActivate: [featureAccessGuard('reports')] },
      { path: 'reports/heatmap', component: HeatmapComponent, canActivate: [featureAccessGuard('reportAnalytics')] },
      { path: 'reports/audit', component: AuditTrailComponent, canActivate: [featureAccessGuard('auditTrail')] },
      { path: 'completed-appointments', component: CompletedAppointmentsComponent, canActivate: [roleGuard('SUPER_ADMIN', 'APPROVER', 'HCM')] },
      { path: 'rejected-appointments', component: RejectedAppointmentsComponent, canActivate: [roleGuard('SUPER_ADMIN', 'APPROVER', 'HCM')] },
      { path: 'admin/departments', component: DepartmentManagementComponent, canActivate: [roleGuard('SUPER_ADMIN')] },
      { path: 'admin/department-requests', component: DepartmentRequestsComponent, canActivate: [roleGuard('SUPER_ADMIN')] },
      { path: 'admin/users', component: UserManagementComponent, canActivate: [featureAccessGuard('userManagement')] },
      { path: 'admin/schemes', component: SchemeManagementComponent, canActivate: [featureAccessGuard('schemeManagement')] },
      { path: 'admin/appointment-types', component: AppointmentTypeManagementComponent, canActivate: [featureAccessGuard('appointmentTypes')] },
      { path: 'hcm/appointments', component: HcmDashboardComponent, canActivate: [roleGuard('SUPER_ADMIN', 'HCM', 'APPROVER', 'ADMIN')] },
    ]
  },
  { path: '**', redirectTo: '' }
];
