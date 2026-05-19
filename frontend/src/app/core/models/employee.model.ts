import { UserRole } from './auth.model';

export interface Employee {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  phone?: string;
  username?: string;
  role?: UserRole;
  roleLabel?: string;
  nequiNumber?: string;
  bankName?: string;
  bankAccountNumber?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeRequest {
  firstName: string;
  lastName: string;
  phone?: string;
  username?: string;
  password?: string;
  role?: UserRole;
  nequiNumber?: string;
  bankName?: string;
  bankAccountNumber?: string;
  active?: boolean;
}
