import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CashShortfall, CashShortfallMonthlySummary } from '../models/cash-shortfall.model';

@Injectable({ providedIn: 'root' })
export class CashShortfallService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cash-shortfalls`;

  findForMonth(year: number, month: number): Observable<CashShortfall[]> {
    return this.http.get<CashShortfall[]>(this.baseUrl, {
      params: { year: String(year), month: String(month) },
    });
  }

  monthlySummary(year: number, month: number): Observable<CashShortfallMonthlySummary[]> {
    return this.http.get<CashShortfallMonthlySummary[]>(`${this.baseUrl}/summary`, {
      params: { year: String(year), month: String(month) },
    });
  }

  settle(id: number, notes?: string): Observable<CashShortfall> {
    return this.http.patch<CashShortfall>(`${this.baseUrl}/${id}/settle`, { notes: notes ?? null });
  }
}
