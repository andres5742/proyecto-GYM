export type MembershipStatus = 'ACTIVE' | 'EXPIRED' | 'SUSPENDED';

export interface Member {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
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
  email: string;
  phone?: string;
  documentId?: string;
  planId?: number | null;
  status?: MembershipStatus;
  membershipStart?: string;
  membershipEnd?: string;
}
