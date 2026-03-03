import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FileUploadResponse {
  success: boolean;
  filePath: string;
  visitorId: number;
  applicationId: string;
  summary?: string;
  error?: string;
}

@Injectable({ providedIn: 'root' })
export class FileService {

  private readonly baseUrl = '/api/files';

  private readonly allowedTypes = ['application/pdf', 'image/jpeg', 'image/png',
    'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
  private readonly maxFileSizeMb = 10;

  constructor(private http: HttpClient) {}

  /**
   * Upload a file for a visitor's appointment.
   * Stores under /uploads/{visitorId}/{applicationId}/
   */
  uploadFile(
    file: File,
    visitorId: number,
    applicationId: string,
    generateSummary = false
  ): Observable<FileUploadResponse> {
    this.validateFile(file);

    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('visitorId', visitorId.toString());
    formData.append('applicationId', applicationId);
    formData.append('generateSummary', generateSummary.toString());

    return this.http.post<FileUploadResponse>(`${this.baseUrl}/upload`, formData);
  }

  /**
   * Upload with progress tracking.
   */
  uploadFileWithProgress(
    file: File,
    visitorId: number,
    applicationId: string,
    generateSummary = false
  ): Observable<HttpEvent<FileUploadResponse>> {
    this.validateFile(file);

    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('visitorId', visitorId.toString());
    formData.append('applicationId', applicationId);
    formData.append('generateSummary', generateSummary.toString());

    return this.http.post<FileUploadResponse>(`${this.baseUrl}/upload`, formData, {
      reportProgress: true,
      observe: 'events'
    });
  }

  /**
   * Get the download URL for a stored file.
   */
  getDownloadUrl(visitorId: number, applicationId: string, filename: string): string {
    return `${this.baseUrl}/download/${visitorId}/${applicationId}/${filename}`;
  }

  /**
   * Client-side file validation before upload.
   */
  validateFile(file: File): void {
    const maxBytes = this.maxFileSizeMb * 1024 * 1024;
    if (file.size > maxBytes) {
      throw new Error(`File size exceeds ${this.maxFileSizeMb} MB limit.`);
    }
    if (!this.allowedTypes.includes(file.type)) {
      throw new Error(`File type '${file.type}' is not allowed. Allowed: pdf, jpg, png, doc, docx.`);
    }
  }

  isAllowedType(file: File): boolean {
    return this.allowedTypes.includes(file.type);
  }
}
