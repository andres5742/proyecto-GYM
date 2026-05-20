import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ProductCredit,
  ProductCreditPayAllRequest,
  ProductCreditPayAllResponse,
  ProductCreditPayment,
  ProductCreditPaymentRequest,
  ProductCreditRequest,
  ProductCreditStatus,
} from '../models/product-credit.model';

@Injectable({ providedIn: 'root' })
export class ProductCreditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/product-credits`;

  findAll(status?: ProductCreditStatus): Observable<ProductCredit[]> {
    const params: Record<string, string> = {};
    if (status) {
      params['status'] = status;
    }
    return this.http.get<ProductCredit[]>(this.baseUrl, { params });
  }

  findById(id: number): Observable<ProductCredit> {
    return this.http.get<ProductCredit>(`${this.baseUrl}/${id}`);
  }

  create(request: ProductCreditRequest): Observable<ProductCredit> {
    return this.http.post<ProductCredit>(this.baseUrl, request);
  }

  registerPayment(
    creditId: number,
    request: ProductCreditPaymentRequest,
  ): Observable<ProductCreditPayment> {
    return this.http.post<ProductCreditPayment>(`${this.baseUrl}/${creditId}/payments`, request);
  }

  payAllForMember(
    memberId: number,
    request: ProductCreditPayAllRequest,
  ): Observable<ProductCreditPayAllResponse> {
    return this.http.post<ProductCreditPayAllResponse>(
      `${this.baseUrl}/member/${memberId}/pay-all`,
      request,
    );
  }
}
