import { fakeAsync, tick } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';

describe('LoginComponent CAPTCHA lifecycle', () => {
  const captcha = { captchaId: 'new-id', captchaImage: 'image', expiresAt: 'later' };

  function setup(loginResult: Observable<boolean>) {
    const auth = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'login', 'user']);
    auth.isLoggedIn.and.returnValue(false);
    auth.login.and.returnValue(loginResult);
    auth.user.and.returnValue(null);
    const captchaService = jasmine.createSpyObj('CaptchaService', ['generate']);
    captchaService.generate.and.returnValue(of(captcha));
    const router = jasmine.createSpyObj('Router', ['navigate']);
    const translate = jasmine.createSpyObj('TranslateService', ['instant']);
    translate.instant.and.callFake((value: string) => value);
    const toast = jasmine.createSpyObj('ToastService', ['error', 'warning']);
    const component = new LoginComponent(auth, captchaService, router, translate, toast);
    component.loginForm.setValue({ username: 'admin', password: 'secret', captchaId: 'old-id', captchaValue: 'OLD' });
    return { component, auth, captchaService, router, toast };
  }

  ['ERR_034', 'ERR_035_LOCKED', 'INVALID_CAPTCHA', 'CAPTCHA_EXPIRED'].forEach(code => {
    it(`refreshes CAPTCHA once after server failure ${code}`, fakeAsync(() => {
      const context = setup(throwError(() => ({ status: 401, error: { errorCode: code, message: 'Login failed' } })));
      const focus = jasmine.createSpy('focus');
      context.component.captchaInput = { nativeElement: { focus } } as never;

      context.component.login();
      tick();

      expect(context.captchaService.generate).toHaveBeenCalledTimes(1);
      expect(context.component.loginForm.controls.username.value).toBe('admin');
      expect(context.component.loginForm.controls.captchaValue.value).toBe('');
      expect(context.component.loginForm.controls.captchaId.value).toBe('new-id');
      expect(focus).toHaveBeenCalledTimes(1);
      expect(context.toast.error.calls.count() + context.toast.warning.calls.count()).toBe(1);
      expect(context.component.loading).toBeFalse();
    }));
  });

  it('does not refresh CAPTCHA after a network failure', () => {
    const context = setup(throwError(() => ({ status: 0 })));
    context.component.login();
    expect(context.captchaService.generate).not.toHaveBeenCalled();
  });

  it('does not refresh CAPTCHA after successful login', () => {
    const context = setup(of(true));
    context.component.login();
    expect(context.captchaService.generate).not.toHaveBeenCalled();
    expect(context.router.navigate).toHaveBeenCalled();
  });

  it('handles CAPTCHA refresh failure without retrying', () => {
    const context = setup(of(true));
    context.captchaService.generate.and.returnValue(throwError(() => new Error('offline')));
    context.component.refreshCaptcha();
    expect(context.captchaService.generate).toHaveBeenCalledTimes(1);
    expect(context.toast.error).toHaveBeenCalledWith('Unable to refresh CAPTCHA. Please try again.');
    expect(context.component.captchaLoading()).toBeFalse();
  });
});
