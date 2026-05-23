export type MembershipPlanKind = 'REGULAR' | 'TIQUETERA';

export interface MembershipPlan {
  id: number;
  name: string;
  description?: string;
  durationDays: number;
  planKind: MembershipPlanKind;
  monthlyEntryLimit?: number | null;
  price: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MembershipPlanRequest {
  name: string;
  description?: string;
  durationDays: number;
  planKind?: MembershipPlanKind;
  monthlyEntryLimit?: number | null;
  price: number;
  active?: boolean;
}

export function isTiqueteraPlan(plan: MembershipPlan): boolean {
  return plan.planKind === 'TIQUETERA';
}

/** Normaliza nombre para comparar (sin acentos, minúsculas). */
export function normalizePlanNameKey(name: string): string {
  return name
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}

const BILLING_DAY_PASS_NAME_KEYS = ['entreno dia', 'bailes deportivos'] as const;

/** Pases F2/F8; no van en el cobro de membresía. */
export function isBillingDayPassPlan(plan: MembershipPlan): boolean {
  const nameKey = normalizePlanNameKey(plan.name);
  if (BILLING_DAY_PASS_NAME_KEYS.some((n) => nameKey === n)) {
    return true;
  }
  const desc = normalizePlanNameKey(plan.description ?? '');
  return (
    desc.includes('(f2') ||
    desc.includes('f2 en recepcion') ||
    desc.includes('/ f3') ||
    desc.includes('f3)') ||
    desc.includes('/ f8') ||
    desc.includes('f8)')
  );
}

export function isMembershipBillablePlan(plan: MembershipPlan): boolean {
  return plan.active && !isBillingDayPassPlan(plan);
}
