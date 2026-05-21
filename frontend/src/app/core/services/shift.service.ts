import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SalesSummary } from '../models/sale.model';
import {
  ShiftDetail,
  ShiftOpenInventoryPreview,
  WorkShift,
  WorkShiftOpenResult,
  WorkShiftRequest,
} from '../models/shift.model';

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

  openInventoryPreview(shiftDate?: string): Observable<ShiftOpenInventoryPreview> {
    const params = shiftDate ? { shiftDate } : undefined;
    return this.http.get<ShiftOpenInventoryPreview>(`${this.baseUrl}/open-inventory-preview`, {
      params,
    });
  }

  open(request: WorkShiftRequest): Observable<WorkShiftOpenResult> {
    return this.http.post<WorkShiftOpenResult>(`${this.baseUrl}/open`, request);
  }

  close(id: number): Observable<WorkShift> {
    return this.http.post<WorkShift>(`${this.baseUrl}/${id}/close`, {});
  }

  getSalesSummary(shiftId: number): Observable<SalesSummary> {
    return this.http.get<SalesSummary>(`${this.baseUrl}/${shiftId}/sales-summary`);
  }

  getDetail(shiftId: number): Observable<ShiftDetail> {
    return this.http.get<ShiftDetail>(`${this.baseUrl}/${shiftId}/detail`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
