import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ShiftHandover, ShiftHandoverRequest } from '../models/shift-handover.model';

@Injectable({ providedIn: 'root' })
export class ShiftHandoverService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/shift-handovers`;

  findAll(): Observable<ShiftHandover[]> {
    return this.http.get<ShiftHandover[]>(this.baseUrl);
  }

  findById(id: number): Observable<ShiftHandover> {
    return this.http.get<ShiftHandover>(`${this.baseUrl}/${id}`);
  }

  previewForShift(workShiftId: number): Observable<ShiftHandover> {
    return this.http.get<ShiftHandover>(`${this.baseUrl}/shift/${workShiftId}`);
  }

  submit(request: ShiftHandoverRequest): Observable<ShiftHandover> {
    return this.http.post<ShiftHandover>(this.baseUrl, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
