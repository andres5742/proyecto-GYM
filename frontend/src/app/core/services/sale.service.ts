import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Sale, SaleRequest, SalesSummary } from '../models/sale.model';

@Injectable({ providedIn: 'root' })
export class SaleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/sales`;

  findAll(employeeId?: number | null, workShiftId?: number | null): Observable<Sale[]> {
    let params = new HttpParams();
    if (employeeId) {
      params = params.set('employeeId', employeeId);
    }
    if (workShiftId) {
      params = params.set('workShiftId', workShiftId);
    }
    return this.http.get<Sale[]>(this.baseUrl, { params });
  }

  getSummary(workShiftId?: number | null): Observable<SalesSummary> {
    let params = new HttpParams();
    if (workShiftId) {
      params = params.set('workShiftId', workShiftId);
    }
    return this.http.get<SalesSummary>(`${this.baseUrl}/summary`, { params });
  }

  create(request: SaleRequest): Observable<Sale> {
    return this.http.post<Sale>(this.baseUrl, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
