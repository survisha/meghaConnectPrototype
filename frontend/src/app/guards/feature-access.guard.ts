import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AccessControlService, AppFeature } from '../services/access-control.service';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../shared/toast/toast.service';

export const featureAccessGuard = (feature: AppFeature): CanActivateFn => () => {
  const auth = inject(AuthService);
  if (auth.user()?.passwordChangeRequired) {
    return inject(Router).createUrlTree(['/change-password']);
  }
  if (inject(AccessControlService).can(feature)) return true;
  inject(ToastService).error('You are not authorized to access this feature.');
  return inject(Router).createUrlTree(['/dashboard']);
};
