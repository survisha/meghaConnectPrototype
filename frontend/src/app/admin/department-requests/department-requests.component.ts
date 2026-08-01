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
interface PageResult<T> { content:T[]; totalElements:number; totalPages:number; number:number; size:number; }
interface ApprovalResult { request:RequestRow; departmentAdmin:{username:string}; oneTimeTemporaryPassword:string; }
@Component({ selector:'app-department-requests', standalone:true,
  imports:[CommonModule,FormsModule,MatButtonModule,MatIconModule],
  templateUrl:'./department-requests.component.html', styleUrls:['./department-requests.component.scss'] })
export class DepartmentRequestsComponent implements OnInit {
  rows:RequestRow[]=[]; loading=false; error=''; status='PENDING'; page=0; size=20; totalElements=0; totalPages=0;
  selected:RequestRow|null=null; rejecting:RequestRow|null=null; rejectionReason=''; actionLoading=false;
  approvedCredentials: { username:string; temporaryPassword:string } | null = null;
  constructor(private readonly http:HttpClient, private readonly toast:ToastService) {}
  ngOnInit():void { this.load(); }
  load():void { if(this.loading)return; this.loading=true; this.error='';
    const query=`?page=${this.page}&size=${this.size}${this.status?`&status=${this.status}`:''}`;
    this.http.get<Wrapped<PageResult<RequestRow>>>(`${environment.apiUrl}/department-access-requests${query}`).subscribe({
      next:r=>{this.rows=r.data?.content??[];this.totalElements=r.data?.totalElements??0;this.totalPages=r.data?.totalPages??0;this.loading=false;},
      error:error=>{this.toast.error(apiErrorMessage(error,'Unable to load department requests'));this.loading=false;} }); }
  filterChanged():void { this.page=0; this.load(); }
  previousPage():void { if(this.page>0){this.page--;this.load();} }
  nextPage():void { if(this.page+1<this.totalPages){this.page++;this.load();} }
  approve(row:RequestRow):void { if(!confirm(`Approve ${row.departmentName}?`))return; this.actionLoading=true;
    this.http.post<Wrapped<ApprovalResult>>(`${environment.apiUrl}/department-access-requests/${row.id}/approve`,{}).subscribe({next:result=>{
      this.approvedCredentials={username:result.data.departmentAdmin.username,temporaryPassword:result.data.oneTimeTemporaryPassword};
      this.toast.success('Department approved and temporary credentials generated.');
      this.actionLoading=false;this.load();},error:error=>{this.actionLoading=false;this.toast.error(apiErrorMessage(error,'Approval failed'));}}); }
  openReject(row:RequestRow):void { this.rejecting=row;this.rejectionReason=''; }
  closeReject():void { if(!this.actionLoading){this.rejecting=null;this.rejectionReason='';} }
  confirmReject():void { const reason=this.rejectionReason.trim(); if(reason.length<5){this.toast.warning('Enter a rejection reason of at least 5 characters.');return;}
    this.actionLoading=true;this.http.post(`${environment.apiUrl}/department-access-requests/${this.rejecting!.id}/reject`,{rejectionReason:reason}).subscribe({next:()=>{this.actionLoading=false;this.closeReject();this.toast.success('Department request rejected.');this.load();},error:error=>{this.actionLoading=false;this.toast.error(apiErrorMessage(error,'Rejection failed'));}}); }
  count(status:string):number{return this.rows.filter(r=>r.requestStatus===status).length;}
}
