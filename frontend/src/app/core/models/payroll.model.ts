export interface PayrollConfig {
  id: number;
  weekdayHourlyRate: number;
  sundayHourlyRate: number;
  updatedAt: string;
}

export interface PayrollConfigRequest {
  weekdayHourlyRate: number;
  sundayHourlyRate: number;
}
