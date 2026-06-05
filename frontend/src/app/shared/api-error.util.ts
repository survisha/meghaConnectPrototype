import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(error: unknown, fallbackMessage = 'An unexpected error occurred.'): string {
  if (error instanceof HttpErrorResponse) {
    return apiErrorBodyMessage(error.error, error.message || fallbackMessage);
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return apiErrorBodyMessage(error, fallbackMessage);
}

export function apiErrorBodyMessage(errorBody: unknown, fallbackMessage = 'An unexpected error occurred.'): string {
  if (typeof errorBody === 'string') {
    try {
      return apiErrorBodyMessage(JSON.parse(errorBody), fallbackMessage);
    } catch {
      return errorBody || fallbackMessage;
    }
  }

  if (errorBody && typeof errorBody === 'object') {
    const payload = errorBody as Record<string, unknown>;
    const errorCode = stringValue(payload['errorCode']);
    const message =
      stringValue(payload['message']) ||
      stringValue(payload['errorMessage']) ||
      stringValue(payload['error']);
    if (message) {
      return errorCode ? `${errorCode}: ${message}` : message;
    }
    if (errorCode) {
      return errorCode;
    }
  }

  return fallbackMessage;
}

function stringValue(value: unknown): string {
  return typeof value === 'string' && value.trim() ? value.trim() : '';
}
