import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Holiday, HolidayRequest } from '../models/holiday.model';

@Injectable({ providedIn: 'root' })
export class HolidayService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/holidays`;

  findByMonth(year: number, month: number): Observable<Holiday[]> {
    const params = new HttpParams().set('year', year).set('month', month);
    return this.http.get<Holiday[]>(this.baseUrl, { params });
  }

  create(request: HolidayRequest): Observable<Holiday> {
    return this.http.post<Holiday>(this.baseUrl, request);
  }

  update(id: number, request: HolidayRequest): Observable<Holiday> {
    return this.http.put<Holiday>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
