import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(error: unknown, fallbackMessage = 'An unexpected error occurred.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 504) return 'Form extraction is taking longer than expected. Please try again.';
    return apiErrorBodyMessage(error.error, statusFallback(error.status, fallbackMessage));
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return apiErrorBodyMessage(error, fallbackMessage);
}

export function apiErrorBodyMessage(errorBody: unknown, fallbackMessage = 'An unexpected error occurred.'): string {
  if (typeof errorBody === 'string') {
    if (looksLikeHtml(errorBody)) return fallbackMessage;
    try {
      return apiErrorBodyMessage(JSON.parse(errorBody), fallbackMessage);
    } catch {
      return safeMessage(errorBody) || fallbackMessage;
    }
  }

  if (errorBody && typeof errorBody === 'object') {
    const payload = errorBody as Record<string, unknown>;
    const errorCode = stringValue(payload['errorCode']);
    const message =
      stringValue(payload['message']) ||
      stringValue(payload['errorMessage']) ||
      stringValue(payload['error']);
    if (message && safeMessage(message)) {
      return errorCode ? `${errorCode}: ${message}` : message;
    }
    if (errorCode) {
      return errorCode;
    }
  }

  return fallbackMessage;
}

export function statusFallback(status: number, fallbackMessage = 'An unexpected error occurred. Please try again.'): string {
  const messages: Record<number, string> = {
    0: 'Unable to connect to the server. Please check your network.',
    400: 'Please check the entered information.',
    401: 'Your session has expired. Please log in again.',
    403: 'You are not authorized to perform this action.',
    404: 'The requested information was not found.',
    409: 'The record already exists or was modified.',
    413: 'The uploaded file is too large.',
    429: 'Too many requests. Please try again shortly.',
    500: 'An unexpected error occurred. Please try again.',
    503: 'The service is temporarily unavailable.',
    504: 'Form extraction is taking longer than expected. Please try again.',
  };
  return messages[status] || fallbackMessage;
}

export function looksLikeHtml(value: string): boolean {
  return /<!doctype\s+html|<html\b|<title>\s*\d{3}\s+[^<]*<\/title>|<body\b/i.test(value);
}

function safeMessage(value: string): string {
  const normalized = value.trim();
  if (!normalized || /\b(sql|select\s+|insert\s+|update\s+|delete\s+|exception|stack trace|jwt|jdbc|java\.)\b/i.test(normalized)) {
    return '';
  }
  return normalized.slice(0, 500);
}

function stringValue(value: unknown): string {
  return typeof value === 'string' && value.trim() ? value.trim() : '';
}
