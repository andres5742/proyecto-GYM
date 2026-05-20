import type { Gender } from './member.model';

export type AccessResult = 'GRANTED' | 'DENIED';

export type BiometricCredentialType = 'FINGERPRINT' | 'FACE';

export type AccessPersonType = 'MEMBER' | 'STAFF';

/** Pantalla torniquete: huella en lector físico o rostro con lector biométrico (cámara). */
export type AccessKioskMode = 'FINGERPRINT' | 'FACE';

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
  FACE: 'Rostro (biométrico)',
};

export const ACCESS_KIOSK_MODE_LABELS: Record<AccessKioskMode, string> = {
  FINGERPRINT: 'Huella',
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
