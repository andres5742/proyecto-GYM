export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'TRAINER';

export interface AuthUser {
  token: string | null;
  employeeId: number;
  fullName: string;
  username: string;
  role: UserRole;
  roleLabel: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string | null;
  employeeId: number;
  fullName: string;
  username: string;
  role: UserRole;
  roleLabel: string;
}

export const ROLE_LABELS: Record<UserRole, string> = {
  SUPER_ADMIN: 'Super administrador',
  ADMIN: 'Administrador',
  TRAINER: 'Entrenador',
};
