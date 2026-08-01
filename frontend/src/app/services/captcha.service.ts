import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CaptchaResponse {
  captchaId: string;
  captchaImage: string;
  captchaText?: string | null;
  expiresAt: string;
}

@Injectable({ providedIn: 'root' })
export class CaptchaService {
  constructor(private readonly http: HttpClient) {}

  generate(): Observable<CaptchaResponse> {
    return this.http.get<CaptchaResponse>(`${environment.apiUrl}/captcha/generate`);
  }
}
