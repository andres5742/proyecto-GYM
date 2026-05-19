import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BusinessDaySchedule, BusinessHours } from '../models/business-hours.model';

@Injectable({ providedIn: 'root' })
export class BusinessHoursService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/business-hours`;

  get(): Observable<BusinessHours> {
    return this.http.get<BusinessHours>(this.baseUrl);
  }

  update(days: BusinessDaySchedule[]): Observable<BusinessHours> {
    return this.http.put<BusinessHours>(this.baseUrl, days);
  }
}
