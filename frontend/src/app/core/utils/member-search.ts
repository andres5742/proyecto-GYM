import { Member } from '../models/member.model';

export function memberDisplayName(m: Member): string {
  return `${m.firstName} ${m.lastName}`.trim();
}

/** Orden alfabético estable (apellido y nombre). */
export function sortMembersByName(members: Member[]): Member[] {
  return [...members].sort((a, b) =>
    memberDisplayName(a).localeCompare(memberDisplayName(b), 'es', { sensitivity: 'base' }),
  );
}

/**
 * Búsqueda de afiliados: primero quienes empiezan por el texto, luego el resto; todo A–Z.
 */
export function filterMembersByQuery(members: Member[], query: string, limit = 25): Member[] {
  const q = query.trim().toLowerCase();
  const sorted = sortMembersByName(members);
  if (!q) {
    return sorted.slice(0, limit);
  }

  const matches = sorted.filter((m) => {
    const name = memberDisplayName(m).toLowerCase();
    const doc = (m.documentId ?? '').toLowerCase();
    const phone = (m.phone ?? '').toLowerCase();
    return name.includes(q) || doc.includes(q) || phone.includes(q);
  });

  const rank = (m: Member): number => {
    const name = memberDisplayName(m).toLowerCase();
    if (name.startsWith(q)) {
      return 0;
    }
    if (name.split(/\s+/).some((w) => w.startsWith(q))) {
      return 1;
    }
    if ((m.documentId ?? '').toLowerCase().startsWith(q)) {
      return 2;
    }
    return 3;
  };

  return matches
    .sort((a, b) => {
      const ra = rank(a);
      const rb = rank(b);
      if (ra !== rb) {
        return ra - rb;
      }
      return memberDisplayName(a).localeCompare(memberDisplayName(b), 'es', { sensitivity: 'base' });
    })
    .slice(0, limit);
}

/** Alias descriptivo. */
export const filterMembersForSearch = filterMembersByQuery;
