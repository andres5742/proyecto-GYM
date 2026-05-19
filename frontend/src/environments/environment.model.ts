export interface Environment {
  production: boolean;
  apiUrl: string;
  /** Base URL for `/uploads/...` (e.g. http://localhost:8081). Omit to use same origin + proxy. */
  uploadsBaseUrl?: string;
  accessDeviceKey?: string;
}
