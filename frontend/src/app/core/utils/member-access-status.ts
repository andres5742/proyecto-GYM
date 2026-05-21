import {
  BiometricEnrollResponse,
  FaceWebcamEnrollResponse,
  isMemberPerson,
} from '../models/access.model';

export interface MemberAccessFlags {
  fingerprint: boolean;
  /** Tarjeta en lector ZKTeco (número Pin / card en el dispositivo). */
  card: boolean;
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
      map[memberId] = { fingerprint: false, card: false, face: false, count: 0 };
    }
    return map[memberId];
  };

  for (const e of enrollments) {
    if (!isMemberPerson(e) || e.memberId == null) {
      continue;
    }
    if (e.credentialType === 'FINGERPRINT') {
      ensure(e.memberId).fingerprint = true;
    }
    if (e.credentialType === 'CARD') {
      ensure(e.memberId).card = true;
    }
  }

  for (const w of webcamEnrollments) {
    if (isMemberPerson(w) && w.memberId != null) {
      ensure(w.memberId).face = true;
    }
  }

  for (const flags of Object.values(map)) {
    flags.count = (flags.fingerprint ? 1 : 0) + (flags.card ? 1 : 0) + (flags.face ? 1 : 0);
  }

  return map;
}

export function memberAccessSummary(flags: MemberAccessFlags | undefined): string {
  if (!flags || flags.count === 0) {
    return 'Sin acceso registrado';
  }
  const max = 3;
  if (flags.count === max) {
    return `Acceso completo (${max}/${max})`;
  }
  const parts: string[] = [];
  if (flags.fingerprint) {
    parts.push('Huella');
  }
  if (flags.card) {
    parts.push('Tarjeta');
  }
  if (flags.face) {
    parts.push('Rostro');
  }
  return `${flags.count}/${max} · ${parts.join(', ')}`;
}
