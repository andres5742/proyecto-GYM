import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FeedbackMessage,
  FeedbackMessageRequest,
  FeedbackStatusUpdateRequest,
} from '../models/feedback.model';

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/feedback`;

  submit(request: FeedbackMessageRequest): Observable<FeedbackMessage> {
    return this.http.post<FeedbackMessage>(this.baseUrl, request);
  }

  findAll(): Observable<FeedbackMessage[]> {
    return this.http.get<FeedbackMessage[]>(this.baseUrl);
  }

  pendingCount(): Observable<{ pending: number }> {
    return this.http.get<{ pending: number }>(`${this.baseUrl}/stats`);
  }

  updateStatus(id: number, request: FeedbackStatusUpdateRequest): Observable<FeedbackMessage> {
    return this.http.patch<FeedbackMessage>(`${this.baseUrl}/${id}/status`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
