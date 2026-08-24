import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LegacyColumn { id?:number; index:number; sourceHeader:string; normalizedHeader:string; detectedType:string; targetField?:string; identifierType?:string; mandatory:boolean; ignored:boolean; mappingStatus:string; }
export interface LegacySheet { id:number; sheetIndex:number; sheetName:string; hidden:boolean; detectedHeaderRow?:number; columnCount:number; rowCount:number; detectedDatasetId?:number; confirmedDatasetId?:number; dataset?:string; targetTable?:string; confidence?:number; validRows:number; importedRows:number; failedRows:number; duplicateRows:number; skippedRows:number; status:string; statusReason?:string; columns:LegacyColumn[]; }
export interface LegacyBatch { batchId:number; fileName:string; status:string; uploadedBy:string; uploadedAt:string; totalSheets:number; analyzedSheets:number; importedSheets:number; failedSheets:number; skippedSheets:number; mappingRequiredSheets:number; totalRows:number; validRows:number; importedRows:number; failedRows:number; duplicateRows:number; sheets:LegacySheet[]; }
export interface LegacyPreview { sheetId:number; sheetName:string; totalRows:number; columns:string[]; rows:string[][]; }
export interface LegacyDataset { id:number; code:string; name:string; category?:string; approved:boolean; columns:Array<{targetField:string;detectedType:string;identifierType:string;mandatory:boolean}>; }
export interface LegacyError { id:number; sheetId:number; sheetName:string; rowNumber:number; columnName?:string; rawValue?:string; errorCode:string; errorMessage:string; }
export interface Page<T>{content:T[];totalElements:number;totalPages:number;number:number;size:number;}

@Injectable({providedIn:'root'})
export class LegacyImportService {
  private readonly base='/api/v1/legacy-import';
  constructor(private readonly http:HttpClient){}
  upload(file:File):Observable<LegacyBatch>{const form=new FormData();form.append('file',file,file.name);return this.http.post<LegacyBatch>(`${this.base}/upload`,form);}
  get(batchId:number):Observable<LegacyBatch>{return this.http.get<LegacyBatch>(`${this.base}/${batchId}`);}
  preview(batchId:number,sheetId:number):Observable<LegacyPreview>{return this.http.get<LegacyPreview>(`${this.base}/${batchId}/sheets/${sheetId}/preview`,{params:new HttpParams().set('limit',20)});}
  map(batchId:number,sheet:LegacySheet,datasetId:number):Observable<LegacySheet>{return this.http.post<LegacySheet>(`${this.base}/${batchId}/sheets/${sheet.id}/mapping`,{datasetId,headerRow:sheet.detectedHeaderRow,columns:sheet.columns.map(c=>({sourceColumnIndex:c.index,targetField:c.targetField,ignored:c.ignored}))});}
  validate(batchId:number):Observable<LegacyBatch>{return this.http.post<LegacyBatch>(`${this.base}/${batchId}/validate`,{});}
  execute(batchId:number):Observable<LegacyBatch>{return this.http.post<LegacyBatch>(`${this.base}/${batchId}/execute`,{});}
  retry(batchId:number,sheetId:number):Observable<LegacySheet>{return this.http.post<LegacySheet>(`${this.base}/${batchId}/sheets/${sheetId}/retry`,{});}
  skip(batchId:number,sheetId:number):Observable<LegacySheet>{return this.http.post<LegacySheet>(`${this.base}/${batchId}/sheets/${sheetId}/skip`,{});}
  history(page=0):Observable<Page<LegacyBatch>>{return this.http.get<Page<LegacyBatch>>(`${this.base}/history`,{params:new HttpParams().set('page',page).set('size',20)});}
  errors(batchId:number,page=0):Observable<Page<LegacyError>>{return this.http.get<Page<LegacyError>>(`${this.base}/${batchId}/errors`,{params:new HttpParams().set('page',page).set('size',100)});}
  datasets():Observable<LegacyDataset[]>{return this.http.get<LegacyDataset[]>('/api/v1/legacy-datasets');}
  createDataset(request:unknown):Observable<LegacyDataset>{return this.http.post<LegacyDataset>('/api/v1/legacy-datasets',request);}
  downloadErrors(batchId:number):Observable<Blob>{return this.http.get(`${this.base}/${batchId}/errors/export`,{responseType:'blob'});}
  downloadSummary(batchId:number):Observable<Blob>{return this.http.get(`${this.base}/${batchId}/summary/export`,{responseType:'blob'});}
}
