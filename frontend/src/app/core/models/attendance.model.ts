export interface WorkAttendance {
  id: number;
  employeeId: number;
  employeeName: string;
  employeePaymentInfo?: string;
  workDate: string;
  clockIn: string;
  clockOut?: string;
  hoursWorked?: number;
  hourlyRateApplied?: number;
  amountOwed?: number;
  sunday: boolean;
  dayTypeLabel: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkAttendanceRequest {
  employeeId: number;
  workDate: string;
  clockIn: string;
  clockOut?: string;
  notes?: string;
}

export interface AttendanceSummary {
  totalRecords: number;
  openRecords: number;
  totalHours: number;
  totalOwed: number;
}
