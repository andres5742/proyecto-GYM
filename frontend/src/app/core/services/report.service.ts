import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DailyBusinessReport, MonthlyBusinessReport } from '../models/report.model';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);

  daily(date?: string): Observable<DailyBusinessReport> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<DailyBusinessReport>(`${environment.apiUrl}/reports/daily`, { params });
  }

  monthly(year: number, month: number): Observable<MonthlyBusinessReport> {
    const params = new HttpParams().set('year', year).set('month', month);
    return this.http.get<MonthlyBusinessReport>(`${environment.apiUrl}/reports/monthly`, { params });
  }
}
