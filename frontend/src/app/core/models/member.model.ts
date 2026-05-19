export type MembershipStatus = 'ACTIVE' | 'EXPIRED' | 'SUSPENDED';

export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export interface Member {
  id: number;
  firstName: string;
  lastName: string;
  gender?: Gender;
  phone?: string;
  documentId?: string;
  planId?: number;
  planName?: string;
  status: MembershipStatus;
  membershipStart?: string;
  membershipEnd?: string;
  createdAt: string;
  updatedAt: string;
}

export interface MemberRequest {
  firstName: string;
  lastName: string;
  phone?: string;
  documentId?: string;
  gender?: Gender | null;
  planId?: number | null;
  status?: MembershipStatus;
  membershipStart?: string;
  membershipEnd?: string;
}
