import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) {
    return auth.user()?.passwordChangeRequired
      ? router.createUrlTree(['/change-password'])
      : true;
  }
  return router.createUrlTree(['/login']);
};
