export interface MembershipPlan {
  id: number;
  name: string;
  description?: string;
  durationDays: number;
  price: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MembershipPlanRequest {
  name: string;
  description?: string;
  durationDays: number;
  price: number;
  active?: boolean;
}
