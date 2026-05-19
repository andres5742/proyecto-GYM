export type ShiftStatus = 'OPEN' | 'CLOSED';

export interface WorkShift {
  id: number;
  shiftDate: string;
  name: string;
  openedAt: string;
  closedAt?: string;
  status: ShiftStatus;
  createdAt: string;
}

export interface WorkShiftRequest {
  name: string;
  shiftDate?: string;
}
