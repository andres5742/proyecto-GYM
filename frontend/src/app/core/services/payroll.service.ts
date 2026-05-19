import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PayrollConfig, PayrollConfigRequest } from '../models/payroll.model';

@Injectable({ providedIn: 'root' })
export class PayrollService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/payroll-config`;

  get(): Observable<PayrollConfig> {
    return this.http.get<PayrollConfig>(this.baseUrl);
  }

  update(request: PayrollConfigRequest): Observable<PayrollConfig> {
    return this.http.put<PayrollConfig>(this.baseUrl, request);
  }
}
