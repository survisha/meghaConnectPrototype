import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly baseUrl = `${this.getFileApiBaseUrl()}/files`;

  constructor(private http: HttpClient) {}

  getPreviewBlob(documentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/preview/${documentId}`, { responseType: 'blob' });
  }

  downloadDocument(documentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/download/${documentId}`, { responseType: 'blob' });
  }

  private getFileApiBaseUrl(): string {
    const apiUrl = environment.apiUrl.replace(/\/+$/, '');
    if (/\/api\/v1$/i.test(apiUrl)) {
      return apiUrl.replace(/\/api\/v1$/i, '/api');
    }
    if (/\/api$/i.test(apiUrl)) {
      return apiUrl;
    }
    return `${apiUrl}/api`;
  }
}
