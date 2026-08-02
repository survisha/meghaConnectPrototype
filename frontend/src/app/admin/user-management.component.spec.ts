import { of, throwError } from 'rxjs';
import { UserManagementComponent } from './user-management.component';

describe('UserManagementComponent department scope', () => {
  function setup(role: 'DEPARTMENT_ADMIN' | 'SUPER_ADMIN' = 'DEPARTMENT_ADMIN') {
    const auth = jasmine.createSpyObj('AuthService', ['hasRole', 'user']);
    auth.hasRole.and.callFake((...roles: string[]) => roles.includes(role));
    auth.user.and.returnValue({
      username: 'health.admin', role, departmentId: role === 'DEPARTMENT_ADMIN' ? 1 : undefined,
      departmentName: role === 'DEPARTMENT_ADMIN' ? 'Health' : undefined,
    });
    const http = jasmine.createSpyObj('HttpClient', ['get', 'post', 'put', 'patch', 'delete']);
    const toast = jasmine.createSpyObj('ToastService', ['error', 'success']);
    const component = new UserManagementComponent(auth, http, toast);
    return { component, auth, http, toast };
  }

  it('loads one backend-scoped page and displays own users', () => {
    const context = setup();
    context.http.get.and.returnValue(of({
      content: [{ id: 2, username: 'health.deo', fullName: 'Health DEO', role: 'DEO', departmentId: 1 }],
      totalElements: 1, totalPages: 1, number: 0, size: 10,
    }));

    context.component.loadUsers();

    expect(context.http.get).toHaveBeenCalledTimes(1);
    expect(context.component.pagedUsers.map(user => user.username)).toEqual(['health.deo']);
    expect(context.component.totalRecords).toBe(1);
    expect(context.component.isLoading).toBeFalse();
    expect(context.component.isDepartmentAdmin).toBeTrue();
    expect(context.component.departmentHeading).toContain('Health');
  });

  it('sends pagination and filters without trusting a department id for Department Admin', () => {
    const context = setup();
    context.component.filters.search = 'deo';
    context.component.filters.department = 'Another Department';
    context.http.get.and.returnValue(of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 }));

    context.component.loadUsers();

    const options = context.http.get.calls.mostRecent().args[1];
    expect(options.params.get('page')).toBe('0');
    expect(options.params.get('size')).toBe('10');
    expect(options.params.get('search')).toBe('deo');
    expect(options.params.has('departmentId')).toBeFalse();
  });

  it('finalizes loader and shows the safe department error state', () => {
    const context = setup();
    context.http.get.and.returnValue(throwError(() => new Error('server details')));

    context.component.loadUsers();

    expect(context.component.isLoading).toBeFalse();
    expect(context.component.errorMsg).toBe('Unable to load department users.');
    expect(context.toast.error).toHaveBeenCalledOnceWith('Unable to load department users.');
  });
});
