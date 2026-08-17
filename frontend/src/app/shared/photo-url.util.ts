import { environment } from '../../environments/environment';

export function resolvePhotoUrl(value?: string | null): string | null {
  const source = value?.trim() || '';
  if (!source) return null;
  if (/^(https?:\/\/|data:image\/|blob:)/i.test(source)) return source;
  if (/^[A-Za-z0-9+/=\r\n]+$/.test(source) && source.length > 80) {
    return `data:image/jpeg;base64,${source.replace(/\s/g, '')}`;
  }

  const api = environment.apiUrl.replace(/\/+$/, '');
  const origin = /^https?:\/\//i.test(api)
    ? new URL(api).origin
    : typeof window !== 'undefined'
      ? window.location.origin
      : '';
  return `${origin}/${source.replace(/^\/+/, '')}`;
}
