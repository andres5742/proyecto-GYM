export type ModuleCode =
  | 'SOCIOS'
  | 'PLANES'
  | 'INVENTARIO'
  | 'VENTAS'
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
