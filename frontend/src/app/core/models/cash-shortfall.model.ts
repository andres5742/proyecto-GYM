export type CashShortfallStatus = 'PENDING' | 'SETTLED';

export interface CashShortfall {
  id: number;
  employeeId: number;
  employeeName: string;
  workShiftId: number;
  workShiftName: string;
  shiftHandoverId?: number | null;
  recordDate: string;
  expectedAmount: number;
  declaredAmount: number;
  shortfallAmount: number;
  status: CashShortfallStatus;
  statusLabel: string;
  notes?: string | null;
  settledAt?: string | null;
  settledByName?: string | null;
  createdAt: string;
}

export interface CashShortfallMonthlySummary {
  employeeId: number;
  employeeName: string;
  pendingTotal: number;
  settledTotal: number;
  recordCount: number;
}
