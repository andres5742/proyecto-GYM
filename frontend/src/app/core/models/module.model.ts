export type ModuleCode =
  | 'SOCIOS'
  | 'PLANES'
  | 'INVENTARIO'
  | 'VENTAS'
  | 'ENTREGA_TURNO'
  | 'JORNADA'
  | 'ENTRENADORES'
  | 'NOMINA'
  | 'CONTENIDO_INICIO'
  | 'BUZON'
  | 'CALIFICACIONES'
  | 'ACCESO'
  | 'MODULOS_SISTEMA'
  | 'PUBLIC_BUZON'
  | 'PUBLIC_CALIFICACIONES'
  | 'PUBLIC_ACCESO';

export interface AppModuleItem {
  code: ModuleCode;
  name: string;
  description?: string;
  category: 'PANEL' | 'PUBLIC';
  categoryLabel: string;
  enabled: boolean;
  sortOrder: number;
}

export type ModuleFlags = Record<string, boolean>;

export type ConfigurableRole = 'TRAINER' | 'ADMIN';

export interface RoleModulePermission {
  moduleCode: ModuleCode;
  moduleName: string;
  description?: string;
  allowed: boolean;
  globallyEnabled: boolean;
}

export const CONFIGURABLE_ROLES: { value: ConfigurableRole; label: string }[] = [
  { value: 'TRAINER', label: 'Entrenador' },
  { value: 'ADMIN', label: 'Administrador' },
];
