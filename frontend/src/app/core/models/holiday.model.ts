export interface Holiday {
  id: number;
  date: string;
  name: string;
  description?: string | null;
  createdAt: string;
}

export interface HolidayRequest {
  date: string;
  name: string;
  description?: string;
}
