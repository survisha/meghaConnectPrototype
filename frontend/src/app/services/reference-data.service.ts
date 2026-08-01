import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ReferenceDataDto {
  code: string;
  value: string;
}

@Injectable({ providedIn: 'root' })
export class ReferenceDataService {
  private readonly baseUrl = `${environment.apiUrl}/reference`;
  private readonly cache = new Map<string, Observable<ReferenceDataDto[]>>();

  constructor(private http: HttpClient) {}

  getByType(type: string, parentCode?: string): Observable<ReferenceDataDto[]> {
    const key = `${type}:${parentCode ?? ''}`;
    let request = this.cache.get(key);
    if (!request) {
      request = this.http.get<ReferenceDataDto[]>(`${this.baseUrl}/${type}`, {
        params: parentCode ? { parentCode } : {}
      }).pipe(shareReplay({ bufferSize: 1, refCount: false }));
      this.cache.set(key, request);
    }
    return request;
  }
}
