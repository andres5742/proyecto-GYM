import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SalesSummary } from '../models/sale.model';
import { WorkShift, WorkShiftRequest } from '../models/shift.model';

@Injectable({ providedIn: 'root' })
export class ShiftService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/shifts`;

  findAll(): Observable<WorkShift[]> {
    return this.http.get<WorkShift[]>(this.baseUrl);
  }

  findOpen(): Observable<WorkShift | null> {
    return this.http
      .get<WorkShift>(`${this.baseUrl}/open`, { observe: 'response' })
      .pipe(map((res) => (res.status === 204 ? null : res.body ?? null)));
  }

  open(request: WorkShiftRequest): Observable<WorkShift> {
    return this.http.post<WorkShift>(`${this.baseUrl}/open`, { name: request.name });
  }

  close(id: number): Observable<WorkShift> {
    return this.http.post<WorkShift>(`${this.baseUrl}/${id}/close`, {});
  }

  getSalesSummary(shiftId: number): Observable<SalesSummary> {
    return this.http.get<SalesSummary>(`${this.baseUrl}/${shiftId}/sales-summary`);
  }
}
