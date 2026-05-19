import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CarouselSlide,
  CarouselSlideRequest,
  GymMediaItem,
  GymMediaItemRequest,
  SiteFooter,
} from '../models/home-content.model';

@Injectable({ providedIn: 'root' })
export class HomeContentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/home`;

  findCarousel(): Observable<CarouselSlide[]> {
    return this.http.get<CarouselSlide[]>(`${this.baseUrl}/carousel`);
  }

  findAllCarousel(): Observable<CarouselSlide[]> {
    return this.http.get<CarouselSlide[]>(`${this.baseUrl}/carousel/all`);
  }

  createCarousel(request: CarouselSlideRequest): Observable<CarouselSlide> {
    return this.http.post<CarouselSlide>(`${this.baseUrl}/carousel`, request);
  }

  updateCarousel(id: number, request: CarouselSlideRequest): Observable<CarouselSlide> {
    return this.http.put<CarouselSlide>(`${this.baseUrl}/carousel/${id}`, request);
  }

  deleteCarousel(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/carousel/${id}`);
  }

  uploadImage(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ url: string }>(`${this.baseUrl}/upload`, formData);
  }

  findMedia(): Observable<GymMediaItem[]> {
    return this.http.get<GymMediaItem[]>(`${this.baseUrl}/media`);
  }

  findAllMedia(): Observable<GymMediaItem[]> {
    return this.http.get<GymMediaItem[]>(`${this.baseUrl}/media/all`);
  }

  createMedia(request: GymMediaItemRequest): Observable<GymMediaItem> {
    return this.http.post<GymMediaItem>(`${this.baseUrl}/media`, request);
  }

  updateMedia(id: number, request: GymMediaItemRequest): Observable<GymMediaItem> {
    return this.http.put<GymMediaItem>(`${this.baseUrl}/media/${id}`, request);
  }

  deleteMedia(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/media/${id}`);
  }

  getFooter(): Observable<SiteFooter> {
    return this.http.get<SiteFooter>(`${this.baseUrl}/footer`);
  }

  updateFooter(footer: SiteFooter): Observable<SiteFooter> {
    return this.http.put<SiteFooter>(`${this.baseUrl}/footer`, footer);
  }
}
