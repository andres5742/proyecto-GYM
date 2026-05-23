import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AccessLogEntry,
  KioskAccessEvent,
  LastDeviceRead,
  AccessVerifyResponse,
  BiometricCredentialType,
  BiometricEnrollRequest,
  BiometricEnrollResponse,
  FaceWebcamEnrollResponse,
} from '../models/access.model';

@Injectable({ providedIn: 'root' })
export class AccessService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/access`;

  verify(
    deviceUserId: string,
    credentialType: BiometricCredentialType = 'FINGERPRINT',
  ): Observable<AccessVerifyResponse> {
    const headers = new HttpHeaders({
      'X-Device-Key': environment.accessDeviceKey ?? '',
    });
    return this.http.post<AccessVerifyResponse>(
      `${this.baseUrl}/verify`,
      { deviceUserId, credentialType },
      { headers },
    );
  }

  /** @deprecated Use verify(id, 'FINGERPRINT') */
  verifyFingerprint(fingerprintUserId: string): Observable<AccessVerifyResponse> {
    return this.verify(fingerprintUserId, 'FINGERPRINT');
  }

  verifyCard(cardOrPin: string): Observable<AccessVerifyResponse> {
    return this.verify(cardOrPin, 'CARD');
  }

  /** Igual que el lector ZKTeco en producción (tarjeta/huella/cédula en Pin). */
  zktEvent(pin: string): Observable<AccessVerifyResponse> {
    const headers = new HttpHeaders({
      'X-Device-Key': environment.accessDeviceKey ?? '',
    });
    return this.http.post<AccessVerifyResponse>(
      `${this.baseUrl}/zkt/event`,
      { pin: pin.trim() },
      { headers },
    );
  }

  /** Pantalla /acceso: elegir afiliado cuando el mismo código de tarjeta está en varias cuentas. */
  zktSelectMember(pin: string, memberId: number): Observable<AccessVerifyResponse> {
    const headers = new HttpHeaders({
      'X-Device-Key': environment.accessDeviceKey ?? '',
    });
    return this.http.post<AccessVerifyResponse>(
      `${this.baseUrl}/zkt/select-member`,
      { pin: pin.trim(), memberId },
      { headers },
    );
  }

  /** Última lectura del lector (registrar tarjeta/huella en recepción). */
  lastDeviceRead(sinceIso: string): Observable<LastDeviceRead | null> {
    const params = new HttpParams().set('since', sinceIso);
    return this.http.get<LastDeviceRead | null>(`${this.baseUrl}/last-read`, { params });
  }

  /** Eventos de acceso desde el lector ZKTeco (pantalla /acceso). */
  /** F2 entreno / F3 bailes: abre torniquete (misma función que atajos en Facturación). */
  kioskOpenGate(reason: 'workout' | 'sports-dance'): Observable<AccessVerifyResponse> {
    const headers = new HttpHeaders({
      'X-Device-Key': environment.accessDeviceKey ?? '',
    });
    return this.http.post<AccessVerifyResponse>(
      `${this.baseUrl}/kiosk/open-gate`,
      { reason },
      { headers },
    );
  }

  kioskEventsSince(sinceIso: string, afterLogId = 0): Observable<KioskAccessEvent[]> {
    const headers = new HttpHeaders({
      'X-Device-Key': environment.accessDeviceKey ?? '',
    });
    let params = new HttpParams().set('since', sinceIso);
    if (afterLogId > 0) {
      params = params.set('afterId', String(afterLogId));
    }
    return this.http.get<KioskAccessEvent[]>(`${this.baseUrl}/kiosk/events`, { headers, params });
  }

  verifyFace(deviceUserId: string): Observable<AccessVerifyResponse> {
    return this.verify(deviceUserId, 'FACE');
  }

  listEnrollments(): Observable<BiometricEnrollResponse[]> {
    return this.http.get<BiometricEnrollResponse[]>(`${this.baseUrl}/enrollments`);
  }

  enroll(request: BiometricEnrollRequest): Observable<BiometricEnrollResponse> {
    return this.http.post<BiometricEnrollResponse>(`${this.baseUrl}/enroll`, request);
  }

  removeEnrollment(
    memberId: number,
    credentialType: BiometricCredentialType = 'FINGERPRINT',
  ): Observable<void> {
    const params = new HttpParams().set('type', credentialType);
    return this.http.delete<void>(`${this.baseUrl}/enroll/${memberId}`, { params });
  }

  removeStaffEnrollment(
    employeeId: number,
    credentialType: BiometricCredentialType = 'FINGERPRINT',
  ): Observable<void> {
    const params = new HttpParams().set('type', credentialType);
    return this.http.delete<void>(`${this.baseUrl}/enroll/staff/${employeeId}`, { params });
  }

  enrollStaff(request: Omit<BiometricEnrollRequest, 'memberId'> & { employeeId: number }): Observable<BiometricEnrollResponse> {
    return this.http.post<BiometricEnrollResponse>(`${this.baseUrl}/enroll`, request);
  }

  logs(): Observable<AccessLogEntry[]> {
    return this.http.get<AccessLogEntry[]>(`${this.baseUrl}/logs`);
  }

  clearLogs(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/logs`);
  }

  deleteLog(logId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/logs/${logId}/revoke`, {});
  }

  manualOpen(memberId: number): Observable<AccessVerifyResponse> {
    return this.http.post<AccessVerifyResponse>(`${this.baseUrl}/manual-open/${memberId}`, {});
  }

  verifyWebcam(descriptor: number[]): Observable<AccessVerifyResponse> {
    const headers = new HttpHeaders({
      'X-Device-Key': environment.accessDeviceKey ?? '',
    });
    return this.http.post<AccessVerifyResponse>(
      `${this.baseUrl}/webcam/verify`,
      { descriptor },
      { headers },
    );
  }

  enrollWebcam(memberId: number, descriptor: number[]): Observable<FaceWebcamEnrollResponse> {
    return this.http.post<FaceWebcamEnrollResponse>(`${this.baseUrl}/webcam/enroll`, {
      memberId,
      descriptor,
    });
  }

  enrollStaffWebcam(employeeId: number, descriptor: number[]): Observable<FaceWebcamEnrollResponse> {
    return this.http.post<FaceWebcamEnrollResponse>(`${this.baseUrl}/webcam/enroll`, {
      employeeId,
      descriptor,
    });
  }

  listWebcamEnrollments(): Observable<FaceWebcamEnrollResponse[]> {
    return this.http.get<FaceWebcamEnrollResponse[]>(`${this.baseUrl}/webcam/enrollments`);
  }

  removeWebcamEnrollment(memberId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/webcam/enroll/${memberId}`);
  }

  removeStaffWebcamEnrollment(employeeId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/webcam/enroll/staff/${employeeId}`);
  }

  /** Actualiza tarjetas antiguas a formato codigo|cedula (una vez). */
  migrateCardCredentials(): Observable<CardCredentialMigrationResult> {
    return this.http.post<CardCredentialMigrationResult>(
      `${this.baseUrl}/migrate-card-credentials`,
      {},
    );
  }
}

export interface CardCredentialMigrationResult {
  membersUpdated: number;
  membersSkipped: number;
  staffUpdated: number;
  staffSkipped: number;
  message: string;
}
