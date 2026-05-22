import { BiometricEnrollResponse, isMemberPerson } from '../models/access.model';

export interface MemberAccessFlags {
  fingerprint: boolean;
  /** Tarjeta en lector ZKTeco (número Pin / card en el dispositivo). */
  card: boolean;
  count: number;
}

export type MemberAccessMap = Record<number, MemberAccessFlags>;

const ACCESS_METHOD_MAX = 2;

export function buildMemberAccessMap(enrollments: BiometricEnrollResponse[]): MemberAccessMap {
  const map: MemberAccessMap = {};

  const ensure = (memberId: number): MemberAccessFlags => {
    if (!map[memberId]) {
      map[memberId] = { fingerprint: false, card: false, count: 0 };
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

  for (const flags of Object.values(map)) {
    flags.count = (flags.fingerprint ? 1 : 0) + (flags.card ? 1 : 0);
  }

  return map;
}

export function memberAccessSummary(flags: MemberAccessFlags | undefined): string {
  if (!flags || flags.count === 0) {
    return 'Sin acceso registrado';
  }
  if (flags.count === ACCESS_METHOD_MAX) {
    return `Acceso completo (${ACCESS_METHOD_MAX}/${ACCESS_METHOD_MAX})`;
  }
  const parts: string[] = [];
  if (flags.fingerprint) {
    parts.push('Huella');
  }
  if (flags.card) {
    parts.push('Tarjeta');
  }
  return `${flags.count}/${ACCESS_METHOD_MAX} · ${parts.join(', ')}`;
}
