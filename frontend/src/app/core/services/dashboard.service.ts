import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UpcomingBirthday } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/dashboard`;

  upcomingBirthdays(withinDays = 7): Observable<UpcomingBirthday[]> {
    return this.http.get<UpcomingBirthday[]>(`${this.baseUrl}/upcoming-birthdays`, {
      params: { withinDays: String(withinDays) },
    });
  }
}
