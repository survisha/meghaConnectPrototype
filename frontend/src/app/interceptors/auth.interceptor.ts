import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, mergeMap, throwError } from 'rxjs';
import { apiErrorBodyMessage } from '../shared/api-error.util';

/**
 * HTTP Interceptor:
 * 1. Attaches JWT Bearer token to all outgoing API requests
 * 2. Handles 401/403 errors (redirect to login)
 * 3. Handles general HTTP errors with user-friendly messages
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const isPublicAuthRequest = isPublicAuthUrl(req.url);

  // Retrieve token from sessionStorage (set by AuthService on login)
  const token = sessionStorage.getItem('megha_token');

  // Clone request with Authorization header if token exists
  const authReq = token && !isPublicAuthRequest
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 0) {
        return throwError(() => new Error('Unable to connect to the server. Please check your network.'));
      }

      if (error.status === 401) {
        const fallback = isPublicAuthRequest
          ? 'Authentication failed. Please check the details and try again.'
          : 'Session expired. Please log in again.';
        if (isPublicAuthRequest) {
          if (error.error instanceof Blob) {
            return from(error.error.text()).pipe(
              mergeMap(text => throwError(() => new Error(apiErrorBodyMessage(text, fallback))))
            );
          }
          return throwError(() => new Error(apiErrorBodyMessage(error.error, fallback)));
        }

        const handleUnauthorized = () => {
          sessionStorage.removeItem('megha_user');
          sessionStorage.removeItem('megha_token');
          if (!req.url.includes('/auth/login')) {
            router.navigate(['/login']);
          }
        };
        if (error.error instanceof Blob) {
          return from(error.error.text()).pipe(
            mergeMap(text => {
              handleUnauthorized();
              return throwError(() => new Error(apiErrorBodyMessage(text, fallback)));
            })
          );
        }
        handleUnauthorized();
        return throwError(() => new Error(apiErrorBodyMessage(error.error, fallback)));
      }

      if (error.status === 403) {
        const fallback = 'You do not have permission to perform this action.';
        if (error.error instanceof Blob) {
          return from(error.error.text()).pipe(
            mergeMap(text => throwError(() => new Error(apiErrorBodyMessage(text, fallback))))
          );
        }
        return throwError(() => new Error(apiErrorBodyMessage(error.error, fallback)));
      }

      if (error.error instanceof Blob) {
        return from(error.error.text()).pipe(
          mergeMap(text => throwError(() => new Error(apiErrorBodyMessage(text, error.message))))
        );
      }

      // Extract error message from API response body if available
      const message = apiErrorBodyMessage(error.error, error.message);
      return throwError(() => new Error(message));
    })
  );
};

function isPublicAuthUrl(url: string): boolean {
  return url.includes('/auth/login') ||
    url.includes('/auth/validate-otp') ||
    url.includes('/visitor/auth/check-mobile') ||
    url.includes('/visitor/auth/check-registration') ||
    url.includes('/visitor/auth/search-registrations') ||
    url.includes('/visitor/auth/generate-otp') ||
    url.includes('/visitor/auth/register');
}
