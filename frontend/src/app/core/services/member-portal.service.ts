import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ChangePasswordRequest, MemberPortalProfile } from '../models/member-portal.model';
import {
  MemberProgressEntry,
  MemberProgressEntryRequest,
} from '../models/member-progress.model';

@Injectable({ providedIn: 'root' })
export class MemberPortalService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/member-portal`;

  getProfile(): Observable<MemberPortalProfile> {
    return this.http.get<MemberPortalProfile>(`${this.baseUrl}/me`);
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/password`, request);
  }

  listProgress(): Observable<MemberProgressEntry[]> {
    return this.http.get<MemberProgressEntry[]>(`${this.baseUrl}/progress`);
  }

  uploadProgressPhoto(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ url: string }>(`${this.baseUrl}/progress/upload`, formData);
  }

  createProgress(request: MemberProgressEntryRequest): Observable<MemberProgressEntry> {
    return this.http.post<MemberProgressEntry>(`${this.baseUrl}/progress`, request);
  }

  updateProgress(id: number, request: MemberProgressEntryRequest): Observable<MemberProgressEntry> {
    return this.http.put<MemberProgressEntry>(`${this.baseUrl}/progress/${id}`, request);
  }

  deleteProgress(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/progress/${id}`);
  }
}
