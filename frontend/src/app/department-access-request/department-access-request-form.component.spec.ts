import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { DepartmentAccessRequestFormComponent } from './department-access-request-form.component';
import { ReferenceDataService } from '../services/reference-data.service';
import { DepartmentAccessRequestService } from '../services/department-access-request.service';
import { ToastService } from '../shared/toast/toast.service';

describe('DepartmentAccessRequestFormComponent', () => {
  let fixture: ComponentFixture<DepartmentAccessRequestFormComponent>;
  let component: DepartmentAccessRequestFormComponent;
  let requests: jasmine.SpyObj<DepartmentAccessRequestService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    requests = jasmine.createSpyObj('DepartmentAccessRequestService', ['submit']);
    toast = jasmine.createSpyObj('ToastService', ['success', 'error', 'warning']);
    await TestBed.configureTestingModule({
      imports: [DepartmentAccessRequestFormComponent],
      providers: [
        { provide: ReferenceDataService, useValue: { getByType: () => of([{ code: 'HEALTH', value: 'Health' }]) } },
        { provide: DepartmentAccessRequestService, useValue: requests },
        { provide: ToastService, useValue: toast },
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DepartmentAccessRequestFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads DEPARTMENT reference values once on initialization', () => {
    expect(component.departments).toEqual([{ code: 'HEALTH', value: 'Health' }]);
  });

  it('keeps invalid fields inline and does not submit', () => {
    component.submit();
    expect(component.form.controls.departmentCode.touched).toBeTrue();
    expect(toast.warning).toHaveBeenCalled();
    expect(requests.submit).not.toHaveBeenCalled();
  });

  it('prevents a second submission while the first is pending', () => {
    requests.submit.and.returnValue(new Subject());
    component.form.setValue({ departmentCode: 'HEALTH', nodalOfficerName: 'Nodal Officer', officialEmail: 'officer@gov.in', officialMobile: '9876543210', requestPurpose: 'Department onboarding request', expectedUserCount: 10, remarks: '' });
    component.submit();
    component.submit();
    expect(requests.submit).toHaveBeenCalledTimes(1);
  });

  it('shows a warning for duplicate pending requests', () => {
    requests.submit.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.setValue({ departmentCode: 'HEALTH', nodalOfficerName: 'Nodal Officer', officialEmail: 'officer@gov.in', officialMobile: '9876543210', requestPurpose: 'Department onboarding request', expectedUserCount: 10, remarks: '' });
    component.submit();
    expect(toast.warning).toHaveBeenCalledWith('A request for the selected department is already pending.');
    expect(component.submitting).toBeFalse();
  });
});
