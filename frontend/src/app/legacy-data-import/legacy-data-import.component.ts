import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { LegacyBatch, LegacyDataset, LegacyError, LegacyImportService, LegacyPreview, LegacySheet } from '../services/legacy-import.service';
import { ToastService } from '../shared/toast/toast.service';
import { Observable } from 'rxjs';

@Component({selector:'app-legacy-data-import',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./legacy-data-import.component.html',styleUrls:['./legacy-data-import.component.scss']})
export class LegacyDataImportComponent implements OnInit {
  file?:File; batch?:LegacyBatch; datasets:LegacyDataset[]=[]; history:LegacyBatch[]=[]; errors:LegacyError[]=[];
  preview?:LegacyPreview; expanded=new Set<number>(); busy=false; error=''; showDatasetForm=false;
  newDataset={datasetCode:'',datasetName:'',category:'',duplicateKeyFields:'',columns:[this.emptyDatasetColumn()]};
  constructor(private readonly api:LegacyImportService,public readonly auth:AuthService,private readonly toast:ToastService){}
  ngOnInit(){this.reloadDatasets();this.loadHistory();}
  get isAdmin(){return this.auth.hasRole('ADMIN');}
  choose(event:Event){const f=(event.target as HTMLInputElement).files?.[0];this.file=f;this.error='';if(f&&!/\.xlsx?$/i.test(f.name)){this.error='Only .xls and .xlsx workbooks are accepted.';this.file=undefined;}}
  upload(){if(!this.file)return;this.run(()=>this.api.upload(this.file!),b=>{this.batch=b;this.toast.success('Workbook analyzed successfully.');this.loadHistory();});}
  toggle(sheet:LegacySheet){this.expanded.has(sheet.id)?this.expanded.delete(sheet.id):this.expanded.add(sheet.id);}
  showPreview(sheet:LegacySheet){if(!this.batch)return;this.run(()=>this.api.preview(this.batch!.batchId,sheet.id),p=>this.preview=p);}
  datasetFor(sheet:LegacySheet){return this.datasets.find(d=>d.id===(sheet.confirmedDatasetId??sheet.detectedDatasetId));}
  targetOptions(sheet:LegacySheet){return this.datasetFor(sheet)?.columns??[];}
  selectDataset(sheet:LegacySheet,id:any){const dataset=this.datasets.find(d=>d.id===Number(id));sheet.confirmedDatasetId=dataset?.id;for(const c of sheet.columns){const exact=dataset?.columns.find(x=>this.norm(x.targetField)===c.normalizedHeader);c.targetField=exact?.targetField;c.ignored=!exact;}}
  saveMapping(sheet:LegacySheet){if(!this.batch||!sheet.confirmedDatasetId)return;this.run(()=>this.api.map(this.batch!.batchId,sheet,sheet.confirmedDatasetId!),updated=>{Object.assign(sheet,updated);this.toast.success(`Mapping saved for ${sheet.sheetName}.`);});}
  validate(){if(!this.batch)return;this.run(()=>this.api.validate(this.batch!.batchId),b=>{this.batch=b;this.toast.success('Workbook validation completed.');this.loadErrors();});}
  execute(){if(!this.batch)return;this.run(()=>this.api.execute(this.batch!.batchId),b=>{this.batch=b;this.toast.success('Workbook import completed.');this.loadErrors();this.loadHistory();});}
  retry(sheet:LegacySheet){if(!this.batch)return;this.run(()=>this.api.retry(this.batch!.batchId,sheet.id),s=>{Object.assign(sheet,s);this.refresh();});}
  skip(sheet:LegacySheet){if(!this.batch)return;this.run(()=>this.api.skip(this.batch!.batchId,sheet.id),s=>{Object.assign(sheet,s);this.refresh();});}
  refresh(){if(!this.batch)return;this.run(()=>this.api.get(this.batch!.batchId),b=>this.batch=b);}
  loadErrors(){if(!this.batch)return;this.api.errors(this.batch.batchId).subscribe({next:p=>this.errors=p.content,error:e=>this.fail(e)});}
  loadHistory(){this.api.history().subscribe({next:p=>this.history=p.content,error:()=>{}});}
  openHistory(item:LegacyBatch){this.run(()=>this.api.get(item.batchId),b=>{this.batch=b;this.loadErrors();});}
  exportErrors(){if(this.batch)this.api.downloadErrors(this.batch.batchId).subscribe(b=>this.save(b,`legacy-import-errors-${this.batch!.batchId}.csv`));}
  exportSummary(){if(this.batch)this.api.downloadSummary(this.batch.batchId).subscribe(b=>this.save(b,`legacy-import-summary-${this.batch!.batchId}.csv`));}
  addDatasetColumn(){this.newDataset.columns.push(this.emptyDatasetColumn());}
  removeDatasetColumn(index:number){if(this.newDataset.columns.length>1)this.newDataset.columns.splice(index,1);}
  createDataset(){const d=this.newDataset;this.run(()=>this.api.createDataset({datasetCode:d.datasetCode,datasetName:d.datasetName,category:d.category,duplicateKeyFields:d.duplicateKeyFields,approved:true,columns:d.columns.map((c,index)=>({targetFieldName:c.fieldName,targetDataType:c.fieldType,identifierType:c.identifierType,mandatory:c.mandatory,displayOrder:index,aliases:c.aliases.split(',').map(x=>x.trim()).filter(Boolean)}))}),()=>{this.showDatasetForm=false;this.newDataset={datasetCode:'',datasetName:'',category:'',duplicateKeyFields:'',columns:[this.emptyDatasetColumn()]};this.reloadDatasets();this.toast.success('Approved dataset definition created.');});}
  statusClass(status:string){return status.toLowerCase().replaceAll('_','-');}
  private reloadDatasets(){this.api.datasets().subscribe({next:d=>this.datasets=d,error:e=>this.fail(e)});}
  private run<T>(call:()=>Observable<T>,next:(value:T)=>void){this.busy=true;this.error='';call().subscribe({next:(v:T)=>{this.busy=false;next(v);},error:(e:any)=>{this.busy=false;this.fail(e);}});}
  private fail(e:any){this.error=e?.error?.message??e?.error?.error??e?.message??'The operation could not be completed.';this.toast.error(this.error);}
  private save(blob:Blob,name:string){const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=name;a.click();URL.revokeObjectURL(url);}
  private norm(v:string){return v.trim().toUpperCase().replace(/[^A-Z0-9]+/g,'_').replace(/^_+|_+$/g,'');}
  private emptyDatasetColumn(){return {fieldName:'',fieldType:'STRING',identifierType:'OTHER',mandatory:false,aliases:''};}
}
