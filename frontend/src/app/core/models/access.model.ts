export type AccessResult = 'GRANTED' | 'DENIED';

export interface AccessVerifyResponse {
  result: AccessResult;
  gateOpened: boolean;
  message: string;
  memberId?: number | null;
  memberName?: string | null;
  fingerprintUserId: string;
}

export interface FingerprintEnrollRequest {
  memberId: number;
  fingerprintUserId: string;
  deviceLabel?: string;
}

export interface FingerprintEnrollResponse {
  memberId: number;
  memberName: string;
  fingerprintUserId: string;
  deviceLabel?: string | null;
  enrolledAt: string;
}

export interface AccessLogEntry {
  id: number;
  fingerprintUserId: string;
  memberId?: number | null;
  memberName?: string | null;
  result: AccessResult;
  resultLabel: string;
  message: string;
  gateOpened: boolean;
  createdAt: string;
}
