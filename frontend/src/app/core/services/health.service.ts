import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class HealthService {
  private readonly http = inject(HttpClient);

  check(): Observable<{ status: string; service: string }> {
    return this.http.get<{ status: string; service: string }>(
      `${environment.apiUrl}/health`,
    );
  }
}
