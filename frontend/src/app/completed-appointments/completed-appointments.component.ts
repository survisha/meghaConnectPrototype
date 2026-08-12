import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';
import { CompletedAppointmentSummary, ExecutiveAppointmentReportService } from '../services/executive-appointment-report.service';
import { DocumentService } from '../services/document.service';
import { ToastService } from '../shared/toast/toast.service';

@Component({selector:'app-completed-appointments',standalone:true,imports:[CommonModule,FormsModule,MatButtonModule,MatIconModule,MatProgressSpinnerModule],templateUrl:'./completed-appointments.component.html',styleUrls:['./completed-appointments.component.scss']})
export class CompletedAppointmentsComponent implements OnInit,OnDestroy {
  rows:CompletedAppointmentSummary[]=[]; total=0; totalPages=0; loading=false; detailLoading=false; detail:any=null;
  photoUrls:Record<number,string>={};
  filters:Record<string,string|number>={applicationId:'',applicantName:'',epic:'',mobile:'',department:'',scheme:'',constituency:'',district:'',fromDate:'',toDate:'',status:'',followUpStatus:'',mla:'',agendaType:'',appointmentType:'',appointmentCategory:'',responsibleOfficer:'',page:0,size:20,sort:'completedAt,desc'};
  constructor(public readonly reports:ExecutiveAppointmentReportService,private readonly documents:DocumentService,private readonly toast:ToastService){}
  ngOnInit(){this.load();}
  load(){this.clearPhotos();this.loading=true;this.reports.completed(this.filters).pipe(finalize(()=>this.loading=false)).subscribe({next:p=>{this.rows=p.content||[];this.total=p.totalElements;this.totalPages=p.totalPages;this.rows.filter(r=>r.photoAvailable).forEach(r=>this.reports.photo(r.appointmentId).subscribe({next:b=>this.photoUrls[r.appointmentId]=URL.createObjectURL(b)}));},error:()=>this.toast.error('Unable to load completed appointments.')});}
  apply(){this.filters['page']=0;this.load();}
  reset(){Object.keys(this.filters).forEach(k=>this.filters[k]='');Object.assign(this.filters,{page:0,size:20,sort:'completedAt,desc'});this.load();}
  page(delta:number){const next=Number(this.filters['page'])+delta;if(next>=0&&next<this.totalPages){this.filters['page']=next;this.load();}}
  view(id:number){this.detailLoading=true;this.reports.completedDetail(id).pipe(finalize(()=>this.detailLoading=false)).subscribe({next:d=>this.detail=d,error:()=>this.toast.error('Unable to load completed appointment details.')});}
  pdf(id:number){this.reports.completedPdf(id).subscribe({next:b=>this.download(b,`completed-appointment-${id}.pdf`),error:()=>this.toast.error('Unable to generate PDF.')});}
  excel(){this.reports.exportCompleted(this.filters).subscribe({next:b=>this.download(b,'completed-appointments.xlsx'),error:()=>this.toast.error('Unable to export Excel.')});}
  excelOne(row:any){this.reports.exportCompleted({applicationId:row.applicationId}).subscribe({next:b=>this.download(b,`completed-appointment-${row.applicationId}.xlsx`),error:()=>this.toast.error('Unable to export appointment Excel.')});}
  previewDocument(d:any){this.documents.getPreviewBlob(Number(d.id)).subscribe({next:(b:Blob)=>{const url=URL.createObjectURL(b);window.open(url,'_blank','noopener');setTimeout(()=>URL.revokeObjectURL(url),60000);},error:()=>this.toast.error('Unable to preview document.')});}
  downloadDocument(d:any){this.documents.downloadDocument(Number(d.id)).subscribe({next:b=>this.download(b,d.filename||'document'),error:()=>this.toast.error('Unable to download document.')});}
  private download(blob:Blob,name:string){const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=name;a.click();URL.revokeObjectURL(url);}
  ngOnDestroy(){this.clearPhotos();}
  private clearPhotos(){Object.values(this.photoUrls).forEach(url=>URL.revokeObjectURL(url));this.photoUrls={};}
}
