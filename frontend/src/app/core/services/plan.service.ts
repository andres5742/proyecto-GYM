import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MembershipPlan, MembershipPlanRequest } from '../models/plan.model';

@Injectable({ providedIn: 'root' })
export class PlanService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/plans`;

  findAll(): Observable<MembershipPlan[]> {
    return this.http.get<MembershipPlan[]>(this.baseUrl);
  }

  create(request: MembershipPlanRequest): Observable<MembershipPlan> {
    return this.http.post<MembershipPlan>(this.baseUrl, request);
  }

  update(id: number, request: MembershipPlanRequest): Observable<MembershipPlan> {
    return this.http.put<MembershipPlan>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
