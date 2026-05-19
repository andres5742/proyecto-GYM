import { Gender, MembershipStatus } from './member.model';

export interface MemberPortalProfile {
  id: number;
  firstName: string;
  lastName: string;
  documentId: string;
  gender: Gender | null;
  phone: string | null;
  planName: string | null;
  status: MembershipStatus;
  statusLabel: string;
  membershipStart: string | null;
  membershipEnd: string | null;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
