import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LegacyDatasetMatch {datasetCode:string;datasetName:string;schemeName?:string;sourceRecordId:number;sourceFile:string;sourceSheet:string;sourceRowNumber:number;details:Record<string,unknown>;}
export interface LegacyPersonCandidate {matchScore:number;matchLevel:'EXACT_EPIC'|'EXACT_MOBILE'|'STRONG'|'POSSIBLE'|'NAME_ONLY';manualVerificationRequired:boolean;matchedOn:string[];legacyPerson:{name?:string;epic?:string;mobile?:string;village?:string;address?:string;district?:string;constituency?:string};datasets:LegacyDatasetMatch[];}
export interface LegacyPersonSearchResponse {page:number;limit:number;totalMatches:number;matches:LegacyPersonCandidate[];}

@Injectable({providedIn:'root'})
export class LegacyPersonSearchService {
  constructor(private readonly http:HttpClient){}
  search(query:Record<string,unknown>):Observable<LegacyPersonSearchResponse>{return this.http.post<LegacyPersonSearchResponse>('/api/v1/legacy-data/search/person',{...query,page:0,limit:20});}
}
