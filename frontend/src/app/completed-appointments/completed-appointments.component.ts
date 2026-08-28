import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { CompletedAppointmentDetail, CompletedAppointmentSummary, ExecutiveAppointmentReportService } from '../services/executive-appointment-report.service';
import { DocumentService } from '../services/document.service';
import { ToastService } from '../shared/toast/toast.service';
import { VisitorHistoryDialogComponent } from '../shared/visitor-history-dialog.component';
import { AppointmentService } from '../services/appointment.service';

@Component({selector:'app-completed-appointments',standalone:true,imports:[CommonModule,FormsModule,MatButtonModule,MatIconModule,MatProgressSpinnerModule,MatDialogModule],templateUrl:'./completed-appointments.component.html',styleUrls:['./completed-appointments.component.scss']})
export class CompletedAppointmentsComponent implements OnInit,OnDestroy {
  rows:CompletedAppointmentSummary[]=[]; total=0; totalPages=0; loading=false; detailLoading=false; detail:CompletedAppointmentDetail|null=null;
  photoUrls:Record<number,string>={};
  actionPanel:'FOLLOW_UP'|'CLOSE'|null=null; actionRemarks=''; actionSaving=false;
  filters:Record<string,string|number>={applicationId:'',applicantName:'',epic:'',mobile:'',department:'',scheme:'',constituency:'',district:'',fromDate:'',toDate:'',status:'',followUpStatus:'',mla:'',agendaType:'',appointmentType:'',appointmentCategory:'',responsibleOfficer:'',page:0,size:20,sort:'completedAt,desc'};
  constructor(public readonly reports:ExecutiveAppointmentReportService,private readonly documents:DocumentService,private readonly toast:ToastService,private readonly dialog:MatDialog,private readonly appointments:AppointmentService){}
  ngOnInit(){this.load();}
  load(){this.clearPhotos();this.loading=true;this.reports.completed(this.filters).pipe(finalize(()=>this.loading=false)).subscribe({next:p=>{this.rows=p.content||[];this.total=p.totalElements;this.totalPages=p.totalPages;this.rows.filter(r=>r.photoAvailable).forEach(r=>this.reports.photo(r.appointmentId).subscribe({next:b=>this.photoUrls[r.appointmentId]=URL.createObjectURL(b)}));},error:()=>this.toast.error('Unable to load completed appointments.')});}
  apply(){this.filters['page']=0;this.load();}
  reset(){Object.keys(this.filters).forEach(k=>this.filters[k]='');Object.assign(this.filters,{page:0,size:20,sort:'completedAt,desc'});this.load();}
  page(delta:number){const next=Number(this.filters['page'])+delta;if(next>=0&&next<this.totalPages){this.filters['page']=next;this.load();}}
  view(id:number){this.detailLoading=true;this.reports.completedDetail(id).pipe(finalize(()=>this.detailLoading=false)).subscribe({next:d=>this.detail=d,error:()=>this.toast.error('Unable to load completed appointment details.')});}
  closeDetail(){this.detail=null;this.actionPanel=null;this.actionRemarks='';}
  viewHistory(){const applicant=this.detail?.applicant;if(applicant?.id)this.dialog.open(VisitorHistoryDialogComponent,{width:'760px',maxWidth:'96vw',data:{citizenId:applicant.id,name:applicant.name}});}
  openActionPanel(action:'FOLLOW_UP'|'CLOSE'){this.actionPanel=action;this.actionRemarks='';}
  cancelActionPanel(){this.actionPanel=null;this.actionRemarks='';}
  saveAction(){const d=this.detail,remarks=this.actionRemarks.trim(),action=this.actionPanel;if(!d||!action||!remarks)return;this.actionSaving=true;const done=()=>this.actionSaving=false;if(action==='CLOSE'){this.appointments.closeAppointment(d.appointment.id,remarks).pipe(finalize(done)).subscribe({next:()=>{this.toast.success('Appointment closed successfully.');this.closeDetail();this.load();},error:()=>this.toast.error('Unable to close appointment.')});}else{this.appointments.addRemark(d.appointment.id,{hcmRemarks:remarks}).pipe(finalize(done)).subscribe({next:()=>{this.toast.success('Follow-up remarks saved successfully.');this.cancelActionPanel();this.view(d.appointment.id);},error:()=>this.toast.error('Unable to save follow-up remarks.')});}}
  pdf(id:number){this.reports.completedPdf(id).subscribe({next:b=>this.download(b,`completed-appointment-${id}.pdf`),error:()=>this.toast.error('Unable to generate PDF.')});}
  excel(){this.reports.exportCompleted(this.filters).subscribe({next:b=>this.download(b,'completed-appointments.xlsx'),error:()=>this.toast.error('Unable to export Excel.')});}
  excelOne(row:any){this.reports.exportCompleted({applicationId:row.applicationId}).subscribe({next:b=>this.download(b,`completed-appointment-${row.applicationId}.xlsx`),error:()=>this.toast.error('Unable to export appointment Excel.')});}
  followUpLabel(value?:string){const normalized=(value||'').trim();return !normalized||normalized==='NONE'?'None':normalized.replace(/_/g,' ').toLowerCase().replace(/\b\w/g,c=>c.toUpperCase());}
  previewDocument(d:any){this.documents.getPreviewBlob(Number(d.id)).subscribe({next:(b:Blob)=>{const url=URL.createObjectURL(b);window.open(url,'_blank','noopener');setTimeout(()=>URL.revokeObjectURL(url),60000);},error:()=>this.toast.error('Unable to preview document.')});}
  downloadDocument(d:any){this.documents.downloadDocument(Number(d.id)).subscribe({next:b=>this.download(b,d.filename||'document'),error:()=>this.toast.error('Unable to download document.')});}
  private download(blob:Blob,name:string){const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=name;a.click();URL.revokeObjectURL(url);}
  ngOnDestroy(){this.clearPhotos();}
  private clearPhotos(){Object.values(this.photoUrls).forEach(url=>URL.revokeObjectURL(url));this.photoUrls={};}
}
