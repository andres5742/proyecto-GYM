import { Environment } from './environment.model';

export const environment: Environment = {
  production: false,
  apiUrl: '/api',
  uploadsBaseUrl: 'http://localhost:8081',
  accessDeviceKey: 'gym-turnstile-dev-key',
};
