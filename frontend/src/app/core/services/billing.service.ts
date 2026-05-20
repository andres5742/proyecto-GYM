import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BillingDailySummary,
  BillingPayment,
  DayWorkoutRegisterRequest,
  DayWorkoutRegisterResponse,
  MembershipPaymentRequest,
} from '../models/billing.model';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/billing`;

  listPayments(date?: string): Observable<BillingPayment[]> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<BillingPayment[]>(`${this.baseUrl}/payments`, { params });
  }

  dailySummary(date?: string): Observable<BillingDailySummary> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<BillingDailySummary>(`${this.baseUrl}/summary/daily`, { params });
  }

  registerDayWorkout(request: DayWorkoutRegisterRequest): Observable<DayWorkoutRegisterResponse> {
    return this.http.post<DayWorkoutRegisterResponse>(`${this.baseUrl}/day-workout/register`, request);
  }

  registerMembershipPayment(request: MembershipPaymentRequest): Observable<BillingPayment> {
    return this.http.post<BillingPayment>(`${this.baseUrl}/membership-payment`, request);
  }

  deletePayment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/payments/${id}`);
  }
}
