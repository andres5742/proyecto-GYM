import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  RatingParticipantAdmin,
  RatingParticipantUpdateRequest,
  TrainerRatingMonthlySummary,
  TrainerRatingParticipant,
  TrainerRatingSubmitRequest,
} from '../models/trainer-rating.model';

@Injectable({ providedIn: 'root' })
export class TrainerRatingService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/trainer-ratings`;

  findParticipants(): Observable<TrainerRatingParticipant[]> {
    return this.http.get<TrainerRatingParticipant[]>(`${this.base}/participants`);
  }

  submit(request: TrainerRatingSubmitRequest): Observable<void> {
    return this.http.post<void>(this.base, request);
  }

  findParticipantsForConfig(): Observable<RatingParticipantAdmin[]> {
    return this.http.get<RatingParticipantAdmin[]>(`${this.base}/participants/all`);
  }

  updateParticipant(
    id: number,
    request: RatingParticipantUpdateRequest,
  ): Observable<RatingParticipantAdmin> {
    return this.http.patch<RatingParticipantAdmin>(`${this.base}/participants/${id}`, request);
  }

  monthlySummary(year: number, month: number): Observable<TrainerRatingMonthlySummary[]> {
    return this.http.get<TrainerRatingMonthlySummary[]>(`${this.base}/monthly`, {
      params: { year: String(year), month: String(month) },
    });
  }
}
