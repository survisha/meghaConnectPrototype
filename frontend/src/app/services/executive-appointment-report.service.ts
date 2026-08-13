import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ReportPage<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }
export interface AppointmentReportFilters { [key: string]: string | number | undefined; }
export interface CompletedAppointmentSummary {
  appointmentId:number; applicationId:string; applicantName:string; epic?:string; mobile?:string; photoAvailable:boolean;
  appointmentCategory:string; appointmentType?:string; department?:string; scheme?:string; constituency?:string; district?:string;
  mla?:string; agendaType?:string; requestedAt?:string; scheduledAt?:string; meetingAt?:string; completedAt?:string;
  directionSummary?:string; assignedDepartment?:string; followUpStatus:string; responsibleOfficer?:string; dueDate?:string; status:string;
}
export interface RejectedAppointmentSummary {
  appointmentId:number; applicationId:string; applicantName:string; epic?:string; mobile?:string; department?:string; scheme?:string;
  constituency?:string; district?:string; mla?:string; agendaType?:string; appointmentType?:string; requestedAt?:string;
  rejectedAt?:string; rejectedBy?:string; rejectionReason?:string; status:string;
}
export interface CompletedAppointmentDetail {
  applicant: { id:number; name?:string; epic?:string; mobile?:string; address?:string; constituency?:string; district?:string; pincode?:string; photoAvailable:boolean };
  appointment: { id:number; applicationId?:string; category?:string; type?:string; source?:string; requestedAt?:string; scheduledAt?:string; meetingAt?:string; completedAt?:string; department?:string; scheme?:string; mla?:string; agendaType?:string; purpose?:string; meetingOutcome?:string; status?:string };
  petitionSummary?:string; approverRemarks?:string; hcmRemarks?:string; forwardedDepartment?:string;
  directions:Array<{ directionId?:string; date?:string; direction?:string; department?:string; officer?:string; dueDate?:string; followUpStatus?:string }>;
  actionItems:Array<{ id:number; directionId?:string; department?:string; officer?:string; instruction?:string; dueDate?:string; status?:string; evidenceRequired?:boolean; escalated?:boolean; completedDate?:string; completionRemarks?:string }>;
  documents:Array<{ id:number; filename?:string; documentType?:string; contentType?:string; fileSizeBytes?:number; uploadedDate?:string; uploadedBy?:string }>;
  aiSummary?:string;
  statusHistory:Array<{ oldStatus?:string; newStatus?:string; action?:string; remarks?:string; performedBy?:string; performedRole?:string; timestamp?:string }>;
}

@Injectable({providedIn:'root'})
export class ExecutiveAppointmentReportService {
  private readonly base=`${environment.apiUrl}/reports`;
  constructor(private readonly http:HttpClient){}
  completed(filters:AppointmentReportFilters):Observable<ReportPage<CompletedAppointmentSummary>>{return this.http.get<ReportPage<CompletedAppointmentSummary>>(`${this.base}/completed-appointments`,{params:this.params(filters)});}
  completedDetail(id:number):Observable<CompletedAppointmentDetail>{return this.http.get<CompletedAppointmentDetail>(`${this.base}/completed-appointments/${id}`);}
  rejected(filters:AppointmentReportFilters):Observable<ReportPage<RejectedAppointmentSummary>>{return this.http.get<ReportPage<RejectedAppointmentSummary>>(`${this.base}/rejected-appointments`,{params:this.params(filters)});}
  rejectedDetail(id:number):Observable<any>{return this.http.get(`${this.base}/rejected-appointments/${id}`);}
  exportCompleted(filters:AppointmentReportFilters):Observable<Blob>{return this.http.get(`${this.base}/completed-appointments/export`,{params:this.params(filters),responseType:'blob'});}
  completedPdf(id:number):Observable<Blob>{return this.http.get(`${this.base}/completed-appointments/${id}/pdf`,{responseType:'blob'});}
  photo(id:number):Observable<Blob>{return this.http.get(`${this.base}/completed-appointments/${id}/photo`,{responseType:'blob'});}
  private params(values:AppointmentReportFilters):HttpParams{let p=new HttpParams();Object.entries(values).forEach(([k,v])=>{if(v!==undefined&&v!==''&&v!==null)p=p.set(k,String(v));});return p;}
}
