import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../environments/environment';
import { ToastService } from '../../shared/toast/toast.service';
import { apiErrorMessage } from '../../shared/api-error.util';

interface RequestRow { id:number; departmentName:string; departmentCode:string; nodalOfficerName:string;
  officialEmail:string; officialMobile:string; requestPurpose:string; expectedUserCount:number; remarks?:string;
  requestStatus:'PENDING'|'APPROVED'|'REJECTED'|'CANCELLED'; submittedAt:string; rejectionReason?:string; }
interface Wrapped<T> { data:T; message:string; }
interface ApprovalResult { request:RequestRow; departmentAdmin:{username:string}; oneTimeTemporaryPassword:string; }
@Component({ selector:'app-department-requests', standalone:true,
  imports:[CommonModule,FormsModule,MatButtonModule,MatIconModule],
  templateUrl:'./department-requests.component.html', styleUrls:['./department-requests.component.scss'] })
export class DepartmentRequestsComponent implements OnInit {
  rows:RequestRow[]=[]; loading=false; error=''; status='';
  approvedCredentials: { username:string; temporaryPassword:string } | null = null;
  constructor(private readonly http:HttpClient, private readonly toast:ToastService) {}
  ngOnInit():void { this.load(); }
  load():void { this.loading=true; this.error=''; const query=this.status?`?status=${this.status}`:'';
    this.http.get<Wrapped<RequestRow[]>>(`${environment.apiUrl}/department-access-requests${query}`).subscribe({
      next:r=>{this.rows=r.data??[];this.loading=false;}, error:error=>{this.toast.error(apiErrorMessage(error,'Unable to load department requests'));this.loading=false;} }); }
  approve(row:RequestRow):void { if(!confirm(`Approve ${row.departmentName}?`))return;
    this.http.post<Wrapped<ApprovalResult>>(`${environment.apiUrl}/department-access-requests/${row.id}/approve`,{}).subscribe({next:result=>{
      this.approvedCredentials={username:result.data.departmentAdmin.username,temporaryPassword:result.data.oneTimeTemporaryPassword};
      this.toast.success('Department approved and temporary credentials generated.');
      this.load();},error:error=>this.toast.error(apiErrorMessage(error,'Approval failed'))}); }
  reject(row:RequestRow):void { const reason=prompt('Enter mandatory rejection reason'); if(!reason?.trim())return;
    this.http.post(`${environment.apiUrl}/department-access-requests/${row.id}/reject`,{rejectionReason:reason.trim()}).subscribe({next:()=>{this.toast.success('Department request rejected.');this.load();},error:error=>this.toast.error(apiErrorMessage(error,'Rejection failed'))}); }
  count(status:string):number{return this.rows.filter(r=>r.requestStatus===status).length;}
}
