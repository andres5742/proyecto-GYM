import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AttendanceSummary,
  WorkAttendance,
  WorkAttendanceRequest,
} from '../models/attendance.model';

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/attendance`;

  findAll(employeeId?: number | null): Observable<WorkAttendance[]> {
    let params = new HttpParams();
    if (employeeId) {
      params = params.set('employeeId', employeeId);
    }
    return this.http.get<WorkAttendance[]>(this.baseUrl, { params });
  }

  getSummary(): Observable<AttendanceSummary> {
    return this.http.get<AttendanceSummary>(`${this.baseUrl}/summary`);
  }

  create(request: WorkAttendanceRequest): Observable<WorkAttendance> {
    return this.http.post<WorkAttendance>(this.baseUrl, request);
  }

  update(id: number, request: WorkAttendanceRequest): Observable<WorkAttendance> {
    return this.http.put<WorkAttendance>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
