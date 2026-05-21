import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  BillingCashRegisterClosePreview,
  BillingCashRegisterCloseResult,
  CloseBillingCashRegisterRequest,
} from '../models/billing-close.model';
import {
  BillingCashRegister,
  BillingCashRegisterExpense,
  BillingCashRegisterExpenseRequest,
  BillingDailySummary,
  BillingMonthlySummary,
  BillingPayment,
  DayWorkoutRegisterRequest,
  DayWorkoutRegisterResponse,
  MembershipOnboardingRequest,
  MembershipOnboardingResponse,
  MembershipPaymentRequest,
  OpenBillingCashRegisterRequest,
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

  registerSportsDance(request: DayWorkoutRegisterRequest): Observable<DayWorkoutRegisterResponse> {
    return this.http.post<DayWorkoutRegisterResponse>(`${this.baseUrl}/sports-dance/register`, request);
  }

  registerMembershipPayment(request: MembershipPaymentRequest): Observable<BillingPayment> {
    return this.http.post<BillingPayment>(`${this.baseUrl}/membership-payment`, request);
  }

  registerMembershipOnboarding(
    request: MembershipOnboardingRequest,
  ): Observable<MembershipOnboardingResponse> {
    return this.http.post<MembershipOnboardingResponse>(`${this.baseUrl}/membership-onboarding`, request);
  }

  deletePayment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/payments/${id}`);
  }

  monthlySummary(year: number, month: number): Observable<BillingMonthlySummary> {
    const params = new HttpParams().set('year', year).set('month', month);
    return this.http.get<BillingMonthlySummary>(`${this.baseUrl}/summary/monthly`, { params });
  }

  findOpenCashRegister(): Observable<BillingCashRegister | null> {
    return this.getCashRegisterOptional(`${this.baseUrl}/cash-registers/open`);
  }

  findTodayCashRegister(): Observable<BillingCashRegister | null> {
    return this.getCashRegisterOptional(`${this.baseUrl}/cash-registers/today`);
  }

  /** 204 / 404 = sin caja (comportamiento normal, no es error). */
  private getCashRegisterOptional(url: string): Observable<BillingCashRegister | null> {
    return this.http
      .get<BillingCashRegister>(url, { observe: 'response' })
      .pipe(
        map((res) => (res.status === 204 ? null : res.body)),
        catchError((err) => {
          if (err.status === 404 || err.status === 204) {
            return of(null);
          }
          return throwError(() => err);
        }),
      );
  }

  listCashRegistersByDate(date?: string): Observable<BillingCashRegister[]> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<BillingCashRegister[]>(`${this.baseUrl}/cash-registers`, { params });
  }

  openCashRegister(request: OpenBillingCashRegisterRequest): Observable<BillingCashRegister> {
    return this.http.post<BillingCashRegister>(`${this.baseUrl}/cash-registers/open`, request);
  }

  closeCashRegisterPreview(id: number): Observable<BillingCashRegisterClosePreview> {
    return this.http.get<BillingCashRegisterClosePreview>(
      `${this.baseUrl}/cash-registers/${id}/close-preview`,
    );
  }

  closeCashRegister(
    id: number,
    request: CloseBillingCashRegisterRequest,
  ): Observable<BillingCashRegisterCloseResult> {
    return this.http.post<BillingCashRegisterCloseResult>(
      `${this.baseUrl}/cash-registers/${id}/close`,
      request,
    );
  }

  listCashRegisterExpenses(date?: string): Observable<BillingCashRegisterExpense[]> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<BillingCashRegisterExpense[]>(`${this.baseUrl}/cash-registers/expenses`, {
      params,
    });
  }

  addCashRegisterExpense(
    request: BillingCashRegisterExpenseRequest,
  ): Observable<BillingCashRegisterExpense> {
    return this.http.post<BillingCashRegisterExpense>(
      `${this.baseUrl}/cash-registers/expenses`,
      request,
    );
  }
}
