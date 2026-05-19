import { environment } from '../../../environments/environment';

function uploadsBaseUrl(): string {
  if (environment.uploadsBaseUrl) {
    return environment.uploadsBaseUrl.replace(/\/$/, '');
  }
  const api = environment.apiUrl;
  if (api.startsWith('http')) {
    return api.replace(/\/api\/?$/, '');
  }
  return '';
}

/** Resolves `/uploads/...` paths to a URL the browser can load. */
export function resolveUploadUrl(url: string | null | undefined): string {
  if (!url?.trim()) {
    return '';
  }
  const trimmed = url.trim();
  if (
    trimmed.startsWith('http://') ||
    trimmed.startsWith('https://') ||
    trimmed.startsWith('data:')
  ) {
    return trimmed;
  }
  if (trimmed.startsWith('/uploads/')) {
    const base = uploadsBaseUrl();
    return base ? `${base}${trimmed}` : trimmed;
  }
  return trimmed;
}
