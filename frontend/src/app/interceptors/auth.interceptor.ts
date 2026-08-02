import { HttpContextToken, HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, mergeMap, throwError } from 'rxjs';
import { apiErrorBodyMessage, statusFallback } from '../shared/api-error.util';
import { AuthSessionService } from '../services/auth-session.service';
import { ToastService } from '../shared/toast/toast.service';

export const SKIP_GLOBAL_ERROR_TOAST = new HttpContextToken<boolean>(() => false);

/**
 * HTTP Interceptor:
 * 1. Attaches JWT Bearer token to all outgoing API requests
 * 2. Handles 401/403 errors (redirect to login)
 * 3. Handles general HTTP errors with user-friendly messages
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authSession = inject(AuthSessionService);
  const toast = inject(ToastService);
  const isPublicAuthRequest = isPublicAuthUrl(req.url);
  const skipGlobalToast = req.context.get(SKIP_GLOBAL_ERROR_TOAST);

  const token = authSession.getAccessToken();
  // Clone request with Authorization header if token exists
  const authReq = token && !isPublicAuthRequest
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 0) {
        if (!skipGlobalToast) toast.error(statusFallback(0));
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
          if (!authSession.beginUnauthorizedHandling()) {
            return;
          }
          authSession.clear();
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
        if (!skipGlobalToast) toast.error(statusFallback(401));
        return throwError(() => new Error(apiErrorBodyMessage(error.error, fallback)));
      }

      if (error.status === 403) {
        const fallback = 'You do not have permission to perform this action.';
        if (!skipGlobalToast) toast.error(statusFallback(403));
        if (error.error instanceof Blob) {
          return from(error.error.text()).pipe(
            mergeMap(text => throwError(() => new Error(apiErrorBodyMessage(text, fallback))))
          );
        }
        return throwError(() => new Error(apiErrorBodyMessage(error.error, fallback)));
      }

      if (!skipGlobalToast && [429, 500, 502, 503, 504].includes(error.status)) {
        const message = apiErrorBodyMessage(error.error, statusFallback(error.status));
        if (error.status === 429) {
          toast.warning(message);
        } else {
          toast.error(message);
        }
      }

      if (error.error instanceof Blob) {
        return from(error.error.text()).pipe(
          mergeMap(text => throwError(() => new Error(apiErrorBodyMessage(text, statusFallback(error.status)))))
        );
      }

      // Extract error message from API response body if available
      const message = apiErrorBodyMessage(error.error, statusFallback(error.status));
      return throwError(() => new Error(message));
    })
  );
};

function isPublicAuthUrl(url: string): boolean {
  return url.includes('/auth/login') ||
    url.includes('/captcha/') ||
    url.includes('/auth/validate-otp') ||
    url.includes('/visitor/auth/check-mobile') ||
    url.includes('/visitor/auth/check-registration') ||
    url.includes('/visitor/auth/search-registrations') ||
    url.includes('/visitor/auth/generate-otp') ||
    url.includes('/visitor/auth/register');
}
