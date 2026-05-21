import type { Gender } from './member.model';

export type AccessResult = 'GRANTED' | 'DENIED';

export type BiometricCredentialType = 'FINGERPRINT' | 'CARD' | 'FACE';

export type AccessPersonType = 'MEMBER' | 'STAFF';

/** Pantalla torniquete: huella, tarjeta ZKTeco o rostro con lector biométrico (cámara). */
export type AccessKioskMode = 'FINGERPRINT' | 'CARD' | 'FACE';

export interface FaceWebcamEnrollResponse {
  memberId?: number | null;
  employeeId?: number | null;
  personType?: AccessPersonType;
  memberName: string;
  documentId?: string | null;
  enrolledAt: string;
}

export interface AccessVerifyResponse {
  result: AccessResult;
  gateOpened: boolean;
  message: string;
  memberId?: number | null;
  employeeId?: number | null;
  personType?: AccessPersonType;
  memberName?: string | null;
  deviceUserId: string;
  /** Alias legacy del API */
  fingerprintUserId?: string;
  credentialType: BiometricCredentialType;
  /** Género del afiliado (null en entrenadores o sin dato). */
  gender?: Gender | null;
  /** Cédula del afiliado cuando el sistema lo identificó. */
  documentId?: string | null;
  /** ID en access_logs (evita duplicar evento en polling). */
  accessLogId?: number | null;
}

export interface BiometricEnrollRequest {
  memberId?: number | null;
  employeeId?: number | null;
  deviceUserId: string;
  /** Alias legacy */
  fingerprintUserId?: string;
  credentialType: BiometricCredentialType;
  deviceLabel?: string;
}

export interface BiometricEnrollResponse {
  memberId?: number | null;
  employeeId?: number | null;
  personType?: AccessPersonType;
  memberName: string;
  deviceUserId: string;
  fingerprintUserId?: string;
  credentialType: BiometricCredentialType;
  credentialTypeLabel: string;
  deviceLabel?: string | null;
  enrolledAt: string;
}

/** @deprecated Use BiometricEnrollResponse */
export type FingerprintEnrollResponse = BiometricEnrollResponse;

/** @deprecated Use BiometricEnrollRequest */
export type FingerprintEnrollRequest = BiometricEnrollRequest;

/** Evento para pantalla /acceso (polling tras ZKTeco). */
export interface KioskAccessEvent {
  id: number;
  deviceUserId: string;
  credentialType: BiometricCredentialType;
  credentialTypeLabel: string;
  memberId?: number | null;
  memberName?: string | null;
  result: AccessResult;
  message: string;
  gateOpened: boolean;
  createdAt: string;
  gender?: Gender | null;
  documentId?: string | null;
}

export interface AccessLogEntry {
  id: number;
  deviceUserId: string;
  fingerprintUserId?: string;
  credentialType: BiometricCredentialType;
  credentialTypeLabel: string;
  memberId?: number | null;
  memberName?: string | null;
  result: AccessResult;
  resultLabel: string;
  message: string;
  gateOpened: boolean;
  createdAt: string;
}

export const BIOMETRIC_TYPE_LABELS: Record<BiometricCredentialType, string> = {
  FINGERPRINT: 'Huella',
  CARD: 'Tarjeta (ZKTeco)',
  FACE: 'Rostro (biométrico)',
};

export const ACCESS_KIOSK_MODE_LABELS: Record<AccessKioskMode, string> = {
  FINGERPRINT: 'Huella',
  CARD: 'Tarjeta',
  FACE: 'Rostro (biométrico)',
};

export function isStaffPerson(
  row: Pick<BiometricEnrollResponse | FaceWebcamEnrollResponse, 'personType' | 'employeeId'>,
): boolean {
  return row.personType === 'STAFF' || row.employeeId != null;
}

export function isMemberPerson(
  row: Pick<BiometricEnrollResponse | FaceWebcamEnrollResponse, 'personType' | 'memberId'>,
): boolean {
  return row.personType !== 'STAFF' && row.memberId != null;
}
