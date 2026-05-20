import { Member } from '../models/member.model';

export function memberDisplayName(member: Member): string {
  const name = `${member.firstName} ${member.lastName}`.trim();
  const doc = member.documentId?.trim();
  return doc ? `${name} — CC ${doc}` : name;
}

export function filterMembersByQuery(members: Member[], query: string, limit = 12): Member[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return members.slice(0, limit);
  }
  return members
    .filter((member) => {
      const haystack = [
        member.firstName,
        member.lastName,
        member.documentId ?? '',
        member.phone ?? '',
        member.planName ?? '',
      ]
        .join(' ')
        .toLowerCase();
      return haystack.includes(q);
    })
    .slice(0, limit);
}
