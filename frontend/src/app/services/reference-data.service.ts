import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ReferenceDataDto {
  code: string;
  value: string;
}

@Injectable({ providedIn: 'root' })
export class ReferenceDataService {
  private readonly baseUrl = `${environment.apiUrl}/reference`;

  constructor(private http: HttpClient) {}

  getByType(type: string): Observable<ReferenceDataDto[]> {
    return this.http.get<ReferenceDataDto[]>(`${this.baseUrl}/${type}`);
  }
}
