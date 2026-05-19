import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AccessLogEntry,
  AccessVerifyResponse,
  FingerprintEnrollRequest,
  FingerprintEnrollResponse,
} from '../models/access.model';

@Injectable({ providedIn: 'root' })
export class AccessService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/access`;

  verifyFingerprint(fingerprintUserId: string): Observable<AccessVerifyResponse> {
    const headers = new HttpHeaders({
      'X-Device-Key': environment.accessDeviceKey ?? '',
    });
    return this.http.post<AccessVerifyResponse>(
      `${this.baseUrl}/verify`,
      { fingerprintUserId },
      { headers },
    );
  }

  listEnrollments(): Observable<FingerprintEnrollResponse[]> {
    return this.http.get<FingerprintEnrollResponse[]>(`${this.baseUrl}/enrollments`);
  }

  enroll(request: FingerprintEnrollRequest): Observable<FingerprintEnrollResponse> {
    return this.http.post<FingerprintEnrollResponse>(`${this.baseUrl}/enroll`, request);
  }

  removeEnrollment(memberId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/enroll/${memberId}`);
  }

  logs(): Observable<AccessLogEntry[]> {
    return this.http.get<AccessLogEntry[]>(`${this.baseUrl}/logs`);
  }

  manualOpen(memberId: number): Observable<AccessVerifyResponse> {
    return this.http.post<AccessVerifyResponse>(`${this.baseUrl}/manual-open/${memberId}`, {});
  }
}
