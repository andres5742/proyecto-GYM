export type CashShortfallStatus = 'PENDING' | 'SETTLED';
export type CashShortfallKind = 'CASH_HANDOVER' | 'INVENTORY' | 'CASH_REGISTER';

export interface InventoryMissingLine {
  productName: string;
  category: string;
  expectedQuantity: number;
  countedQuantity: number;
  missingQuantity: number;
  unitPrice: number;
  lineAmount: number;
}

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
  kind: CashShortfallKind;
  kindLabel: string;
  notes?: string | null;
  inventoryMissingLines: InventoryMissingLine[];
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
