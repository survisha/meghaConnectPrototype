import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * HTTP Interceptor:
 * 1. Attaches JWT Bearer token to all outgoing API requests
 * 2. Handles 401/403 errors (redirect to login)
 * 3. Handles general HTTP errors with user-friendly messages
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  // Retrieve token from sessionStorage (set by AuthService on login)
  const token = sessionStorage.getItem('megha_token');

  // Clone request with Authorization header if token exists
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Unauthorized – clear session and redirect to login
        sessionStorage.removeItem('megha_user');
        sessionStorage.removeItem('megha_token');
        router.navigate(['/login']);
        return throwError(() => new Error('Session expired. Please log in again.'));
      }

      if (error.status === 403) {
        return throwError(() => new Error('You do not have permission to perform this action.'));
      }

      if (error.status === 0) {
        return throwError(() => new Error('Unable to connect to the server. Please check your network.'));
      }

      // Extract error message from API response body if available
      const message = error.error?.message || error.error?.error || error.message || 'An unexpected error occurred.';
      return throwError(() => new Error(message));
    })
  );
};
