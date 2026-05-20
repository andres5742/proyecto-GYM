import {
  BiometricEnrollResponse,
  FaceWebcamEnrollResponse,
  isMemberPerson,
} from '../models/access.model';

export interface MemberAccessFlags {
  fingerprint: boolean;
  /** Lector biométrico de rostro (cámara en entrada). */
  face: boolean;
  count: number;
}

export type MemberAccessMap = Record<number, MemberAccessFlags>;

export function buildMemberAccessMap(
  enrollments: BiometricEnrollResponse[],
  webcamEnrollments: FaceWebcamEnrollResponse[],
): MemberAccessMap {
  const map: MemberAccessMap = {};

  const ensure = (memberId: number): MemberAccessFlags => {
    if (!map[memberId]) {
      map[memberId] = { fingerprint: false, face: false, count: 0 };
    }
    return map[memberId];
  };

  for (const e of enrollments) {
    if (isMemberPerson(e) && e.memberId != null && e.credentialType === 'FINGERPRINT') {
      ensure(e.memberId).fingerprint = true;
    }
  }

  for (const w of webcamEnrollments) {
    if (isMemberPerson(w) && w.memberId != null) {
      ensure(w.memberId).face = true;
    }
  }

  for (const flags of Object.values(map)) {
    flags.count = (flags.fingerprint ? 1 : 0) + (flags.face ? 1 : 0);
  }

  return map;
}

export function memberAccessSummary(flags: MemberAccessFlags | undefined): string {
  if (!flags || flags.count === 0) {
    return 'Sin acceso registrado';
  }
  if (flags.count === 2) {
    return 'Acceso completo (2/2)';
  }
  const parts: string[] = [];
  if (flags.fingerprint) {
    parts.push('Huella');
  }
  if (flags.face) {
    parts.push('Rostro');
  }
  return `${flags.count}/2 · ${parts.join(', ')}`;
}
