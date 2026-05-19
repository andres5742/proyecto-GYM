import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AttendanceSummary,
  WorkAttendance,
  WorkAttendanceRequest,
} from '../models/attendance.model';

export interface AttendanceQuery {
  employeeId?: number;
  year?: number;
  month?: number;
  date?: string;
}

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/attendance`;

  findAll(query: AttendanceQuery = {}): Observable<WorkAttendance[]> {
    let params = new HttpParams();
    if (query.employeeId != null) {
      params = params.set('employeeId', query.employeeId);
    }
    if (query.year != null) {
      params = params.set('year', query.year);
    }
    if (query.month != null) {
      params = params.set('month', query.month);
    }
    if (query.date) {
      params = params.set('date', query.date);
    }
    return this.http.get<WorkAttendance[]>(this.baseUrl, { params });
  }

  getSummary(query: AttendanceQuery = {}): Observable<AttendanceSummary> {
    let params = new HttpParams();
    if (query.employeeId != null) {
      params = params.set('employeeId', query.employeeId);
    }
    if (query.year != null) {
      params = params.set('year', query.year);
    }
    if (query.month != null) {
      params = params.set('month', query.month);
    }
    if (query.date) {
      params = params.set('date', query.date);
    }
    return this.http.get<AttendanceSummary>(`${this.baseUrl}/summary`, { params });
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
