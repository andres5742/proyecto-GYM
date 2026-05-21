import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type DataCleanupScopeCode =
  | 'BILLING'
  | 'SALES_AND_SHIFTS'
  | 'FIADO'
  | 'ACCESS_LOGS'
  | 'MEMBER_BIOMETRICS'
  | 'MEMBERS'
  | 'FEEDBACK'
  | 'TRAINER_RATINGS'
  | 'WORK_ATTENDANCE'
  | 'WALL_POSTS'
  | 'ALL_OPERATIONAL';

export interface DataCleanupScope {
  code: DataCleanupScopeCode;
  label: string;
  description: string;
}

export interface DataCleanupResult {
  scope: string;
  totalDeleted: number;
  details: Record<string, number>;
}

@Injectable({ providedIn: 'root' })
export class DataCleanupService {
  static readonly CONFIRM_PHRASE = 'LIMPIAR';

  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/admin/data-cleanup`;

  listScopes(): Observable<DataCleanupScope[]> {
    return this.http.get<DataCleanupScope[]>(`${this.base}/scopes`);
  }

  cleanup(scope: DataCleanupScopeCode, confirmPhrase: string): Observable<DataCleanupResult> {
    return this.http.post<DataCleanupResult>(this.base, { scope, confirmPhrase });
  }
}
