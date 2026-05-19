import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { WallPost, WallPostRequest } from '../models/wall-post.model';

@Injectable({ providedIn: 'root' })
export class WallPostService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/wall-posts`;

  findActive(): Observable<WallPost[]> {
    return this.http.get<WallPost[]>(this.baseUrl);
  }

  findAllForAdmin(): Observable<WallPost[]> {
    return this.http.get<WallPost[]>(`${this.baseUrl}/all`);
  }

  create(request: WallPostRequest): Observable<WallPost> {
    return this.http.post<WallPost>(this.baseUrl, request);
  }

  update(id: number, request: WallPostRequest): Observable<WallPost> {
    return this.http.put<WallPost>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
