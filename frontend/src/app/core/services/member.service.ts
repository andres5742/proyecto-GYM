import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MemberImportResult } from '../models/member-import.model';
import { Member, MemberRequest } from '../models/member.model';

@Injectable({ providedIn: 'root' })
export class MemberService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/members`;

  findAll(): Observable<Member[]> {
    return this.http.get<Member[]>(this.baseUrl);
  }

  create(request: MemberRequest): Observable<Member> {
    return this.http.post<Member>(this.baseUrl, request);
  }

  update(id: number, request: MemberRequest): Observable<Member> {
    return this.http.put<Member>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  deleteAll(): Observable<{ deleted: number }> {
    return this.http.delete<{ deleted: number }>(this.baseUrl);
  }

  importFromExcel(file: File): Observable<MemberImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<MemberImportResult>(`${this.baseUrl}/import`, formData);
  }

  setPortalPassword(id: number, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}/portal-password`, { newPassword });
  }

  resetPortalPassword(id: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/portal-password/reset`, null);
  }

  freezeMembership(id: number): Observable<Member> {
    return this.http.post<Member>(`${this.baseUrl}/${id}/freeze-membership`, null);
  }

  unfreezeMembership(id: number): Observable<Member> {
    return this.http.post<Member>(`${this.baseUrl}/${id}/unfreeze-membership`, null);
  }
}
